<?php
/**
 * RutasApp Android · API v1.1.0
 * ─────────────────────────────
 * Ruta:  /rutasproapk/api.php
 * BD:    cqvkelal_rutasapp_android
 * Auth:  header X-Auth-Token (64 hex chars)
 *
 * Endpoints S01:
 *   POST register_individual  -- cuenta personal + owner
 *   POST register_company     -- cuenta empresa + owner
 *   POST register_with_invite -- nuevo miembro con invite_code
 *   POST login                -- username|email + password + device
 *   POST logout               -- invalida token
 *   GET  me                   -- estado completo del usuario
 *   POST token_refresh        -- renueva token próximo a expirar
 *   GET  health               -- healthcheck público
 */

declare(strict_types=1);
define('RA_START', microtime(true));

// ── Manejo global de errores -- nunca devolver HTML ──────────
set_exception_handler(function (\Throwable $e) {
    if (!headers_sent()) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
    }
    echo json_encode(['ok' => false, 'error' => 'Error interno del servidor']);
    exit;
});

set_error_handler(function (int $errno, string $errstr) {
    throw new \ErrorException($errstr, 0, $errno);
});

// ── Headers ─────────────────────────────────────────────────
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-Auth-Token');
header('X-Content-Type-Options: nosniff');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// ── Config ──────────────────────────────────────────────────
define('DB_HOST', 'localhost');
define('DB_NAME',
       'cqvkelal_rutasapp_android');
define('DB_USER','cqvkelal_raprousr');
// usuario MySQL con permisos solo en esta BD
define('DB_PASS',      'qd>$.L{!.J');
define('SESSION_DAYS', 30);
define('RATE_WIN',     60);  // ventana en segundos
define('RATE_LOGIN',   10);  // máx intentos de login por ventana
define('RATE_API',     200); // máx requests generales por ventana

// ── DB ──────────────────────────────────────────────────────
function db(): \PDO {
    static $pdo = null;
    if ($pdo) return $pdo;
    $pdo = new \PDO(
        'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
        DB_USER, DB_PASS,
        [
            \PDO::ATTR_ERRMODE            => \PDO::ERRMODE_EXCEPTION,
            \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
            \PDO::ATTR_EMULATE_PREPARES   => false,
        ]
    );
    return $pdo;
}

// ── Sanitización centralizada ────────────────────────────────
function san(mixed $v, int $max = 255): string {
    return mb_substr(trim((string)$v), 0, $max);
}
function sanEmail(string $v): string {
    return mb_strtolower(san($v, 255));
}
function sanInt(mixed $v): int {
    return (int)$v;
}

// ── Rate limiting ────────────────────────────────────────────
function rateCheck(string $key, int $max): void {
    try {
        $now = time();
        $win = $now - RATE_WIN;
        $db  = db();
        $st  = $db->prepare('SELECT COUNT(*) FROM rate_limits WHERE `key`=? AND ts>=?');
        $st->execute([$key, $win]);
        if ((int)$st->fetchColumn() >= $max) {
            http_response_code(429);
            header('Retry-After: ' . RATE_WIN);
            echo json_encode(['ok' => false, 'error' => 'Demasiadas peticiones. Espera un momento.']);
            exit;
        }
        $db->prepare('INSERT INTO rate_limits (`key`, ts) VALUES (?,?)')->execute([$key, $now]);
        // Limpiar entradas antiguas ocasionalmente
        if (rand(1, 50) === 1) {
            $db->prepare('DELETE FROM rate_limits WHERE ts < ?')->execute([$win]);
        }
    } catch (\Throwable) {
        // Si la tabla falla, no bloquear
    }
}

// ── Logging ──────────────────────────────────────────────────
function apiLog(string $action, int $userId = 0, int $accountId = 0,
                int $code = 200, ?string $err = null): void {
    try {
        $ms = (int)((microtime(true) - RA_START) * 1000);
        db()->prepare(
            'INSERT INTO api_logs(action,user_id,account_id,ip,method,status,error_msg,duration_ms)
             VALUES(?,?,?,?,?,?,?,?)'
        )->execute([
            $action, $userId, $accountId,
            $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0',
            $_SERVER['REQUEST_METHOD'] ?? 'GET',
            $code, $err, min($ms, 32767)
        ]);
    } catch (\Throwable) {}
}

// ── Respuestas ───────────────────────────────────────────────
function ok(array $data): void {
    echo json_encode(['ok' => true] + $data);
    exit;
}
function err(string $msg, int $code = 400, string $action = '', int $uid = 0, int $aid = 0): void {
    http_response_code($code);
    if ($action) apiLog($action, $uid, $aid, $code, $msg);
    echo json_encode(['ok' => false, 'error' => $msg]);
    exit;
}

// ── IP del cliente ───────────────────────────────────────────
function clientIP(): string {
    foreach (['HTTP_CF_CONNECTING_IP','HTTP_X_FORWARDED_FOR','REMOTE_ADDR'] as $k) {
        if (!empty($_SERVER[$k])) {
            return trim(explode(',', $_SERVER[$k])[0]);
        }
    }
    return '0.0.0.0';
}

// ── Auth ─────────────────────────────────────────────────────
function requireAuth(): array {
    $token = $_SERVER['HTTP_X_AUTH_TOKEN'] ?? '';
    if (strlen($token) !== 64) err('No autenticado', 401);

    $st = db()->prepare(
        'SELECT s.*, u.id as uid, u.account_id, u.username, u.email, u.name,
                u.role, u.active, a.type as account_type, a.name as account_name,
                a.slug, a.plan, a.plus_config, a.form_config, a.ai_settings
         FROM sessions s
         JOIN users    u ON u.id = s.user_id
         JOIN accounts a ON a.id = u.account_id
         WHERE s.token = ? AND s.expires_at > NOW()'
    );
    $st->execute([$token]);
    $row = $st->fetch();

    if (!$row)             err('Sesión expirada o inválida', 401);
    if (!$row['active'])   err('Cuenta desactivada',         403);

    // Actualizar last_used_at
    db()->prepare('UPDATE sessions SET last_used_at=NOW() WHERE token=?')->execute([$token]);

    return $row;
}

function roleLevel(string $role): int {
    return ['viewer'=>1,'agent'=>2,'manager'=>3,'admin'=>4,'owner'=>5,'god'=>6][$role] ?? 0;
}

// ── Slug generator ───────────────────────────────────────────
function makeSlug(string $name): string {
    $slug = strtolower(preg_replace('/[^a-z0-9]+/i', '-', $name));
    $slug = trim($slug, '-');
    $slug = mb_substr($slug, 0, 80);
    // Comprobar unicidad
    $base = $slug;
    $i    = 0;
    while (true) {
        $try  = $i ? "{$base}-{$i}" : $base;
        $st   = db()->prepare('SELECT 1 FROM accounts WHERE slug=? LIMIT 1');
        $st->execute([$try]);
        if (!$st->fetchColumn()) return $try;
        $i++;
    }
}

// ── Crear sesión ─────────────────────────────────────────────
function createSession(int $userId, array $body): string {
    $token     = bin2hex(random_bytes(32)); // 64 chars
    $expiresAt = date('Y-m-d H:i:s', strtotime('+' . SESSION_DAYS . ' days'));
    $deviceId  = san($body['device_id']  ?? 'unknown', 255);
    $deviceName= san($body['device_name']?? null, 255);
    $platform  = in_array($body['platform'] ?? '', ['android','ios','web'])
                 ? $body['platform'] : 'android';
    $appVer    = san($body['app_version'] ?? null, 30);
    $fcmToken  = isset($body['fcm_token']) ? san($body['fcm_token'], 4096) : null;

    // Upsert -- un solo token por (user, device)
    db()->prepare(
        'INSERT INTO sessions (user_id, token, device_id, device_name, platform,
                               app_version, fcm_token, expires_at)
         VALUES (?,?,?,?,?,?,?,?)
         ON DUPLICATE KEY UPDATE
             token      = VALUES(token),
             device_name= VALUES(device_name),
             app_version= VALUES(app_version),
             fcm_token  = COALESCE(VALUES(fcm_token), fcm_token),
             expires_at = VALUES(expires_at),
             last_used_at = NOW()'
    )->execute([$userId, $token, $deviceId, $deviceName, $platform,
                $appVer, $fcmToken, $expiresAt]);

    return $token;
}

// ── Datos de respuesta de usuario ────────────────────────────
function userResponse(int $userId): array {
    $st = db()->prepare(
        'SELECT u.id, u.username, u.email, u.name, u.role, u.avatar_url,
                u.account_id, u.created_at,
                a.type as account_type, a.name as account_name, a.slug,
                a.plan, a.plus_config, a.form_config, a.ai_settings,
                p.prefs
         FROM users u
         JOIN accounts a ON a.id = u.account_id
         LEFT JOIN user_prefs p ON p.user_id = u.id
         WHERE u.id = ?'
    );
    $st->execute([$userId]);
    $row = $st->fetch();

    return [
        'user' => [
            'id'           => (int)$row['id'],
            'username'     => $row['username'],
            'email'        => $row['email'],
            'name'         => $row['name'],
            'role'         => $row['role'],
            'avatar_url'   => $row['avatar_url'],
            'account_id'   => (int)$row['account_id'],
            'created_at'   => $row['created_at'],
        ],
        'account' => [
            'id'           => (int)$row['account_id'],
            'type'         => $row['account_type'],
            'name'         => $row['account_name'],
            'slug'         => $row['slug'],
            'plan'         => $row['plan'],
            'plus_config'  => $row['plus_config']  ? json_decode($row['plus_config'],  true) : null,
            'form_config'  => $row['form_config']  ? json_decode($row['form_config'],  true) : null,
            'ai_settings'  => $row['ai_settings']  ? json_decode($row['ai_settings'],  true) : null,
        ],
        'prefs' => $row['prefs'] ? (object)json_decode($row['prefs'], true) : (object)[],
    ];
}

// ════════════════════════════════════════════════════════════════
// ROUTER
// ════════════════════════════════════════════════════════════════
$action = san($_GET['action'] ?? '', 60);
$body   = json_decode(file_get_contents('php://input'), true) ?? [];
$ip     = clientIP();

// Rate limit general (no en health)
if ($action !== 'health') {
    rateCheck("api:{$ip}", RATE_API);
}

// ── health ───────────────────────────────────────────────────
if ($action === 'health') {
    try {
        db()->query('SELECT 1');
        $dbOk = true;
    } catch (\Throwable) {
        $dbOk = false;
    }
    if (!$dbOk) http_response_code(503);
    echo json_encode([
        'ok'          => $dbOk,
        'db'          => $dbOk,
        'version'     => '1.1.0',
        'server_time' => date('c'),
    ]);
    exit;
}

// ── register_individual ──────────────────────────────────────
if ($action === 'register_individual') {
    rateCheck("reg:{$ip}", 5); // máx 5 registros por minuto por IP

    $name     = san($body['name']     ?? '', 255);
    $username = san($body['username'] ?? '', 100);
    $email    = sanEmail($body['email']    ?? '');
    $password = $body['password'] ?? '';

    if (!$name)                    err('name es obligatorio',     400, $action);
    if (!$username)                err('username es obligatorio', 400, $action);
    if (!$email || !filter_var($email, FILTER_VALIDATE_EMAIL))
                                   err('email inválido',          400, $action);
    if (strlen($password) < 8)    err('La contraseña debe tener al menos 8 caracteres', 400, $action);

    // Validar formato username
    if (!preg_match('/^[a-z0-9_]{3,50}$/i', $username))
        err('username solo puede contener letras, números y guión bajo (3-50 chars)', 422, $action);

    // Comprobar unicidad
    $st = db()->prepare('SELECT 1 FROM users WHERE username=? OR email=? LIMIT 1');
    $st->execute([$username, $email]);
    if ($st->fetchColumn()) err('El username o email ya está en uso', 409, $action);

    $hash = password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);
    $slug = makeSlug($name);

    db()->beginTransaction();
    try {
        // Crear account
        db()->prepare(
            'INSERT INTO accounts (type, name, slug) VALUES (?,?,?)'
        )->execute(['individual', $name, $slug]);
        $accountId = (int)db()->lastInsertId();

        // Crear user owner
        db()->prepare(
            'INSERT INTO users (account_id, username, email, password_hash, name, role)
             VALUES (?,?,?,?,?,?)'
        )->execute([$accountId, $username, $email, $hash, $name, 'owner']);
        $userId = (int)db()->lastInsertId();

        // Prefs vacías
        db()->prepare(
            'INSERT INTO user_prefs (user_id, prefs) VALUES (?, ?)'
        )->execute([$userId, '{}']);

        $token = createSession($userId, $body);
        db()->commit();
    } catch (\Throwable $e) {
        db()->rollBack();
        if (str_contains($e->getMessage(), 'Duplicate')) {
            err('El username o email ya está en uso', 409, $action);
        }
        throw $e;
    }

    // Actualizar last_login_at
    db()->prepare('UPDATE users SET last_login_at=NOW() WHERE id=?')->execute([$userId]);
    apiLog($action, $userId, $accountId);

    ok(['token' => $token, 'expires_in_days' => SESSION_DAYS] + userResponse($userId));
}

// ── register_company ─────────────────────────────────────────
if ($action === 'register_company') {
    rateCheck("reg:{$ip}", 5);

    $companyName = san($body['company_name'] ?? '', 255);
    $name        = san($body['name']         ?? '', 255);
    $username    = san($body['username']     ?? '', 100);
    $email       = sanEmail($body['email']   ?? '');
    $password    = $body['password'] ?? '';

    if (!$companyName)          err('company_name es obligatorio', 400, $action);
    if (strlen($companyName)<2) err('company_name demasiado corto', 422, $action);
    if (!$name)                 err('name es obligatorio',          400, $action);
    if (!$username)             err('username es obligatorio',      400, $action);
    if (!$email || !filter_var($email, FILTER_VALIDATE_EMAIL))
                                err('email inválido',               400, $action);
    if (strlen($password) < 8) err('La contraseña debe tener al menos 8 caracteres', 400, $action);
    if (!preg_match('/^[a-z0-9_]{3,50}$/i', $username))
        err('username solo puede contener letras, números y guión bajo (3-50 chars)', 422, $action);

    $st = db()->prepare('SELECT 1 FROM users WHERE username=? OR email=? LIMIT 1');
    $st->execute([$username, $email]);
    if ($st->fetchColumn()) err('El username o email ya está en uso', 409, $action);

    $hash = password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);
    $slug = makeSlug($companyName);

    db()->beginTransaction();
    try {
        db()->prepare(
            'INSERT INTO accounts (type, name, slug) VALUES (?,?,?)'
        )->execute(['company', $companyName, $slug]);
        $accountId = (int)db()->lastInsertId();

        db()->prepare(
            'INSERT INTO users (account_id, username, email, password_hash, name, role)
             VALUES (?,?,?,?,?,?)'
        )->execute([$accountId, $username, $email, $hash, $name, 'owner']);
        $userId = (int)db()->lastInsertId();

        db()->prepare(
            'INSERT INTO user_prefs (user_id, prefs) VALUES (?, ?)'
        )->execute([$userId, '{}']);

        $token = createSession($userId, $body);
        db()->commit();
    } catch (\Throwable $e) {
        db()->rollBack();
        if (str_contains($e->getMessage(), 'Duplicate'))
            err('El username o email ya está en uso', 409, $action);
        throw $e;
    }

    db()->prepare('UPDATE users SET last_login_at=NOW() WHERE id=?')->execute([$userId]);
    apiLog($action, $userId, $accountId);

    ok(['token' => $token, 'expires_in_days' => SESSION_DAYS] + userResponse($userId));
}

// ── register_with_invite ─────────────────────────────────────
if ($action === 'register_with_invite') {
    rateCheck("reg:{$ip}", 5);

    $inviteCode = san($body['invite_code'] ?? '', 20);
    $name       = san($body['name']        ?? '', 255);
    $username   = san($body['username']    ?? '', 100);
    $email      = sanEmail($body['email']  ?? '');
    $password   = $body['password'] ?? '';

    if (!$inviteCode) err('invite_code es obligatorio', 400, $action);
    if (!$name)       err('name es obligatorio',        400, $action);
    if (!$username)   err('username es obligatorio',    400, $action);
    if (!$email || !filter_var($email, FILTER_VALIDATE_EMAIL))
                      err('email inválido',             400, $action);
    if (strlen($password) < 8)
        err('La contraseña debe tener al menos 8 caracteres', 400, $action);

    // Validar invite_code
    $st = db()->prepare(
        'SELECT * FROM invite_codes WHERE code=? AND uses_left>0 AND expires_at>NOW() LIMIT 1'
    );
    $st->execute([$inviteCode]);
    $invite = $st->fetch();
    if (!$invite) err('Código de invitación inválido o expirado', 404, $action);

    $st2 = db()->prepare('SELECT 1 FROM users WHERE username=? OR email=? LIMIT 1');
    $st2->execute([$username, $email]);
    if ($st2->fetchColumn()) err('El username o email ya está en uso', 409, $action);

    $hash = password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);

    db()->beginTransaction();
    try {
        db()->prepare(
            'INSERT INTO users (account_id, username, email, password_hash, name, role)
             VALUES (?,?,?,?,?,?)'
        )->execute([
            $invite['account_id'], $username, $email, $hash, $name,
            $invite['role_to_assign']
        ]);
        $userId = (int)db()->lastInsertId();

        db()->prepare(
            'INSERT INTO user_prefs (user_id, prefs) VALUES (?,?)'
        )->execute([$userId, '{}']);

        // Decrementar usos
        db()->prepare(
            'UPDATE invite_codes SET uses_left = uses_left - 1 WHERE id=?'
        )->execute([$invite['id']]);

        $token = createSession($userId, $body);
        db()->commit();
    } catch (\Throwable $e) {
        db()->rollBack();
        if (str_contains($e->getMessage(), 'Duplicate'))
            err('El username o email ya está en uso', 409, $action);
        throw $e;
    }

    db()->prepare('UPDATE users SET last_login_at=NOW() WHERE id=?')->execute([$userId]);
    apiLog($action, $userId, (int)$invite['account_id']);

    ok(['token' => $token, 'expires_in_days' => SESSION_DAYS]
       + userResponse($userId));
}

// ── login ────────────────────────────────────────────────────
if ($action === 'login') {
    rateCheck("login:{$ip}", RATE_LOGIN);

    $credential = san($body['username'] ?? $body['email'] ?? '', 255);
    $password   = $body['password'] ?? '';
    $deviceId   = san($body['device_id'] ?? 'unknown', 255);

    if (!$credential) err('username o email es obligatorio', 400, $action);
    if (!$password)   err('password es obligatorio',         400, $action);

    // Buscar por username o email
    $isEmail = str_contains($credential, '@');
    $field   = $isEmail ? 'email' : 'username';
    $lookup  = $isEmail ? strtolower($credential) : $credential;

    $st = db()->prepare("SELECT * FROM users WHERE {$field}=? LIMIT 1");
    $st->execute([$lookup]);
    $user = $st->fetch();

    // Registrar intento
    $success = $user && $user['active'] && password_verify($password, $user['password_hash']);
    db()->prepare(
        'INSERT INTO login_attempts (ip_address, username_tried, success) VALUES (?,?,?)'
    )->execute([$ip, $credential, $success ? 1 : 0]);

    if (!$user || !password_verify($password, $user['password_hash']))
        err('Usuario o contraseña incorrectos', 401, $action);
    if (!$user['active'])
        err('Cuenta desactivada. Contacta con tu administrador.', 403, $action);

    $token = createSession((int)$user['id'], $body);
    db()->prepare('UPDATE users SET last_login_at=NOW() WHERE id=?')->execute([$user['id']]);
    apiLog($action, (int)$user['id'], (int)$user['account_id']);

    ok(['token' => $token, 'expires_in_days' => SESSION_DAYS]
       + userResponse((int)$user['id']));
}

// ── logout ───────────────────────────────────────────────────
if ($action === 'logout') {
    $token = $_SERVER['HTTP_X_AUTH_TOKEN'] ?? '';
    if (strlen($token) === 64) {
        $clearFcm = ($body['clear_fcm'] ?? true) !== false;
        if ($clearFcm) {
            db()->prepare('UPDATE sessions SET fcm_token=NULL WHERE token=?')->execute([$token]);
        }
        db()->prepare('UPDATE sessions SET expires_at=NOW() WHERE token=?')->execute([$token]);
    }
    ok([]);
}

// ── me ───────────────────────────────────────────────────────
if ($action === 'me') {
    $sess = requireAuth();

    // ¿Token expira en menos de 5 días? Devolver nuevo token automáticamente
    $expiresAt  = strtotime($sess['expires_at']);
    $fiveDays   = time() + (5 * 86400);
    $newToken   = null;
    if ($expiresAt < $fiveDays) {
        $newToken = bin2hex(random_bytes(32));
        $newExpiry = date('Y-m-d H:i:s', strtotime('+' . SESSION_DAYS . ' days'));
        db()->prepare(
            'UPDATE sessions SET token=?, expires_at=? WHERE token=?'
        )->execute([$newToken, $newExpiry, $sess['token']]);
    }

    apiLog($action, (int)$sess['uid'], (int)$sess['account_id']);
    $resp = userResponse((int)$sess['uid']);
    if ($newToken) $resp['new_token'] = $newToken;
    $resp['server_time'] = date('c');
    ok($resp);
}

// ── token_refresh ─────────────────────────────────────────────
if ($action === 'token_refresh') {
    $sess      = requireAuth();
    $newToken  = bin2hex(random_bytes(32));
    $newExpiry = date('Y-m-d H:i:s', strtotime('+' . SESSION_DAYS . ' days'));

    // Actualizar FCM si se proporciona
    $fcm = isset($body['fcm_token']) ? san($body['fcm_token'], 4096) : null;
    db()->prepare(
        'UPDATE sessions SET token=?, expires_at=?,
         fcm_token=COALESCE(?,fcm_token), last_used_at=NOW()
         WHERE token=?'
    )->execute([$newToken, $newExpiry, $fcm, $sess['token']]);

    apiLog($action, (int)$sess['uid'], (int)$sess['account_id']);
    ok(['token' => $newToken, 'expires_at' => $newExpiry]);
}

// ── Acción desconocida ────────────────────────────────────────

// ── routes_list ──────────────────────────────────────────────
if ($action === 'routes_list') {
    $sess  = requireAuth();
    $date  = san($_GET['date']  ?? '', 10);
    $since = san($_GET['since'] ?? '', 30);

    $where  = 'r.user_id = ? AND r.deleted_at IS NULL';
    $params = [(int)$sess['uid']];

    if ($date)  { $where .= ' AND r.date_assigned = ?'; $params[] = $date; }
    if ($since) { $where .= ' AND r.updated_at > ?';    $params[] = $since; }

    $st = db()->prepare(
        "SELECT r.*,
                COUNT(s.id) as stop_count,
                SUM(CASE WHEN s.status='done' THEN 1 ELSE 0 END) as done_count
         FROM routes r
         LEFT JOIN stops s ON s.route_id = r.id AND s.deleted_at IS NULL
         WHERE {$where}
         GROUP BY r.id
         ORDER BY r.date_assigned DESC, r.name ASC
         LIMIT 100"
    );
    $st->execute($params);

    $routes = array_map(fn($r) => [
        'id'            => (int)$r['id'],
        'uid'           => $r['uid'],
        'name'          => $r['name'],
        'date_assigned' => $r['date_assigned'],
        'status'        => $r['status'],
        'notes'         => $r['notes'],
        'stop_count'    => (int)$r['stop_count'],
        'done_count'    => (int)$r['done_count'],
        'created_at'    => $r['created_at'],
        'updated_at'    => $r['updated_at'],
        'deleted_at'    => $r['deleted_at'],
    ], $st->fetchAll());

    apiLog($action, (int)$sess['uid'], (int)$sess['account_id']);
    ok(['routes' => $routes, 'server_time' => date('c')]);
}

// ── delta_sync ───────────────────────────────────────────────
if ($action === 'delta_sync') {
    $sess  = requireAuth();
    $since = san($_GET['since'] ?? '', 30);
    if (!$since) err('Parámetro since es obligatorio', 400, $action);

    $uid = (int)$sess['uid'];
    $aid = (int)$sess['account_id'];

    // Managers y owners ven rutas de todo el account
    $roleLevel = roleLevel($sess['role']);
    if ($roleLevel >= 3) {
        $stR = db()->prepare(
            'SELECT * FROM routes WHERE account_id=? AND updated_at > ? ORDER BY updated_at ASC LIMIT 200'
        );
        $stR->execute([$aid, $since]);
    } else {
        $stR = db()->prepare(
            'SELECT * FROM routes WHERE user_id=? AND updated_at > ? ORDER BY updated_at ASC LIMIT 200'
        );
        $stR->execute([$uid, $since]);
    }

    // Stops: managers ven todos los del account
    $stopsWhere = ($roleLevel >= 3) ? 'r.account_id=?' : 'r.user_id=?';
    $stopsParam = ($roleLevel >= 3) ? $aid : $uid;
    $stS = db()->prepare(
        "SELECT s.id, s.uid, s.route_id, r.uid AS route_uid, s.account_id,
                s.name, s.address, s.lat, s.lng, s.order_index,
                s.external_id, s.contact_name, s.contact_phone,
                s.visit_frequency, s.priority, s.segment, s.account_status, s.opening_hours,
                s.status, s.notes, s.visited_at, s.visit_result, s.next_action, s.pdv_open, s.pdv_inactive,
                s.created_at, s.updated_at, s.deleted_at
         FROM stops s
         JOIN routes r ON r.id = s.route_id
         WHERE {$stopsWhere} AND s.updated_at > ?
         ORDER BY s.updated_at ASC LIMIT 500"
    );
    $stS->execute([$stopsParam, $since]);

    // Jornadas del usuario en el período
    $stD = db()->prepare(
        'SELECT route_uid, date_str, state, started_at, elapsed_ms,
                distance_km, last_lat, last_lng, updated_at
         FROM day_sessions
         WHERE user_id=? AND updated_at > ?
         ORDER BY updated_at ASC LIMIT 200'
    );
    // updated_at en day_sessions es epoch ms; convertir $since (ISO8601) a ms
    $sinceMs = (int)(strtotime($since) * 1000);
    $stD->execute([$uid, $sinceMs]);

    // KPI values de stops actualizados en el período
    $stK = db()->prepare(
        'SELECT kv.stop_uid, kv.kpi_id, kv.value_text, kv.updated_at
         FROM kpi_values kv
         JOIN stops s ON s.uid = kv.stop_uid
         JOIN routes r ON r.id = s.route_id
         WHERE r.user_id=? AND kv.updated_at > ?
         ORDER BY kv.updated_at ASC LIMIT 1000'
    );
    $stK->execute([$uid, $since]);

    apiLog($action, $uid, $aid);
    ok([
        'routes'       => $stR->fetchAll(),
        'stops'        => $stS->fetchAll(),
        'day_sessions' => $stD->fetchAll(),
        'kpi_values'   => $stK->fetchAll(),
        'server_time'  => date('c'),
    ]);
}

// ── batch_sync ───────────────────────────────────────────────
if ($action === 'batch_sync') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];
    $ops  = $body['operations'] ?? [];

    $synced = [];
    $errors = [];

    db()->beginTransaction();
    try {
        foreach ($ops as $op) {
            $entity    = san($op['entity']    ?? '', 20);
            $operation = san($op['operation'] ?? '', 10);
            $data      = $op['data'] ?? [];
            $clientUid = san($op['uid'] ?? '', 36);

            try {
                if ($entity === 'route') {
                    if ($operation === 'create' || $operation === 'update') {
                        $db = db();
                        $db->prepare(
                            'INSERT INTO routes
                                (uid, account_id, user_id, name, date_assigned, status, notes, created_at, updated_at)
                             VALUES (?,?,?,?,?,?,?,?,?)
                             ON DUPLICATE KEY UPDATE
                                name=VALUES(name), status=VALUES(status),
                                notes=VALUES(notes), updated_at=VALUES(updated_at)'
                        )->execute([
                            $clientUid, $aid, $uid,
                            san($data['name'] ?? '', 255),
                            san($data['date_assigned'] ?? date('Y-m-d'), 10),
                            san($data['status'] ?? 'pending', 20),
                            san($data['notes'] ?? '', 5000) ?: null,
                            san($data['created_at'] ?? date('c'), 30),
                            date('c'),
                        ]);
                        $serverId = (int)$db->lastInsertId() ?: null;
                        $db->prepare(
                            'INSERT INTO sync_log (account_id, user_id, entity, entity_uid, operation)
                             VALUES (?,?,?,?,?)'
                        )->execute([$aid, $uid, 'route', $clientUid, $operation]);
                        $synced[] = ['uid' => $clientUid, 'server_id' => $serverId, 'entity' => 'route'];

                    } elseif ($operation === 'delete') {
                        db()->prepare(
                            'UPDATE routes SET deleted_at=NOW(), updated_at=NOW() WHERE uid=? AND user_id=?'
                        )->execute([$clientUid, $uid]);
                        $synced[] = ['uid' => $clientUid, 'entity' => 'route', 'deleted' => true];
                    }

                } elseif ($entity === 'day_session') {
                    if ($operation === 'upsert' || $operation === 'create' || $operation === 'update') {
                        db()->prepare(
                            'INSERT INTO day_sessions
                                (account_id, user_id, route_uid, date_str, state,
                                 started_at, elapsed_ms, distance_km, last_lat, last_lng, updated_at)
                             VALUES (?,?,?,?,?,?,?,?,?,?,?)
                             ON DUPLICATE KEY UPDATE
                                state=VALUES(state), elapsed_ms=VALUES(elapsed_ms),
                                distance_km=VALUES(distance_km),
                                last_lat=VALUES(last_lat), last_lng=VALUES(last_lng),
                                started_at=COALESCE(started_at, VALUES(started_at)),
                                updated_at=VALUES(updated_at), synced_at=NOW()'
                        )->execute([
                            $aid, $uid,
                            san($data['routeUid']    ?? '', 36),
                            san($data['dateStr']     ?? date('Y-m-d'), 10),
                            san($data['state']       ?? 'idle', 10),
                            isset($data['startedAt']) ? (int)$data['startedAt'] : null,
                            (int)($data['elapsedMs']   ?? 0),
                            (float)($data['distanceKm'] ?? 0),
                            isset($data['lastLat']) ? (float)$data['lastLat'] : null,
                            isset($data['lastLng']) ? (float)$data['lastLng'] : null,
                            (int)($data['updatedAt'] ?? 0),
                        ]);
                        $synced[] = ['uid' => $clientUid, 'entity' => 'day_session'];
                    }

                } elseif ($entity === 'kpi_values') {
                    if ($operation === 'upsert' || $operation === 'create' || $operation === 'update') {
                        $stopUid = san($data['stopUid'] ?? '', 36);
                        $values  = $data['values'] ?? [];
                        if ($stopUid && is_array($values)) {
                            $stmt = db()->prepare(
                                'INSERT INTO kpi_values (account_id, stop_uid, kpi_id, value_text)
                                 VALUES (?,?,?,?)
                                 ON DUPLICATE KEY UPDATE value_text=VALUES(value_text), updated_at=NOW()'
                            );
                            foreach ($values as $kpiId => $val) {
                                $stmt->execute([
                                    $aid,
                                    $stopUid,
                                    san((string)$kpiId, 100),
                                    san((string)($val ?? ''), 5000),
                                ]);
                            }
                            $synced[] = ['uid' => $clientUid, 'entity' => 'kpi_values'];
                        } else {
                            $errors[] = ['uid' => $clientUid, 'entity' => 'kpi_values', 'error' => 'stopUid o values inválidos'];
                        }
                    }

                } elseif ($entity === 'business_profile') {
                    if ($operation === 'upsert' || $operation === 'create' || $operation === 'update') {
                        db()->prepare(
                            'INSERT INTO business_profiles (account_id, sector, name, updated_at)
                             VALUES (?,?,?,?)
                             ON DUPLICATE KEY UPDATE
                                sector=VALUES(sector), name=VALUES(name), updated_at=VALUES(updated_at)'
                        )->execute([
                            $aid,
                            san($data['sector'] ?? 'custom', 50),
                            san($data['name']   ?? 'Mi negocio', 255),
                            (int)($data['updatedAt'] ?? 0),
                        ]);
                        $synced[] = ['uid' => $clientUid, 'entity' => 'business_profile'];
                    }

                } elseif ($entity === 'stop') {
                    if ($operation === 'create' || $operation === 'update') {
                        $st = db()->prepare('SELECT id FROM routes WHERE uid=? AND account_id=? LIMIT 1');
                        $st->execute([san($data['route_uid'] ?? '', 36), $aid]);
                        $routeId = $st->fetchColumn();
                        if (!$routeId) {
                            $errors[] = ['uid' => $clientUid, 'entity' => 'stop', 'error' => 'route_uid no encontrado'];
                            continue;
                        }
                        db()->prepare(
                            'INSERT INTO stops
                                (uid, route_id, account_id, name, address, lat, lng,
                                 order_index, status, notes, visited_at,
                                 external_id, contact_name, contact_phone,
                                 visit_result, next_action, pdv_open, pdv_inactive,
                                 created_at, updated_at)
                             VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                             ON DUPLICATE KEY UPDATE
                                name=VALUES(name), address=VALUES(address),
                                lat=VALUES(lat), lng=VALUES(lng),
                                order_index=VALUES(order_index), status=VALUES(status),
                                notes=VALUES(notes), visited_at=VALUES(visited_at),
                                external_id=VALUES(external_id),
                                contact_name=VALUES(contact_name),
                                contact_phone=VALUES(contact_phone),
                                visit_result=VALUES(visit_result),
                                next_action=VALUES(next_action),
                                pdv_open=VALUES(pdv_open),
                                pdv_inactive=VALUES(pdv_inactive),
                                account_status=IF(VALUES(pdv_inactive)=1,'inactive',account_status),
                                updated_at=VALUES(updated_at)'
                        )->execute([
                            $clientUid, $routeId, $aid,
                            san($data['name'] ?? '', 255),
                            san($data['address'] ?? '', 500) ?: null,
                            isset($data['lat']) ? (float)$data['lat'] : null,
                            isset($data['lng']) ? (float)$data['lng'] : null,
                            (int)($data['order_index'] ?? 0),
                            san($data['status'] ?? 'pending', 20),
                            san($data['notes'] ?? '', 5000) ?: null,
                            $data['visited_at'] ?? null,
                            san($data['external_id'] ?? '', 100) ?: null,
                            san($data['contact_name'] ?? '', 255) ?: null,
                            san($data['contact_phone'] ?? '', 50) ?: null,
                            san($data['visit_result'] ?? '', 20) ?: null,
                            san($data['next_action'] ?? '', 5000) ?: null,
                            isset($data['pdv_open']) ? (int)(bool)$data['pdv_open'] : 1,
                            isset($data['pdv_inactive']) ? (int)(bool)$data['pdv_inactive'] : 0,
                            san($data['created_at'] ?? date('c'), 30),
                            date('c'),
                        ]);
                        // Registrar en sync_log
                        db()->prepare(
                            'INSERT INTO sync_log (account_id, user_id, entity, entity_uid, operation)
                             VALUES (?,?,?,?,?)'
                        )->execute([$aid, $uid, 'stop', $clientUid, $operation]);
                        $synced[] = ['uid' => $clientUid, 'entity' => 'stop'];

                    } elseif ($operation === 'delete') {
                        db()->prepare(
                            'UPDATE stops SET deleted_at=NOW(), updated_at=NOW() WHERE uid=? AND account_id=?'
                        )->execute([$clientUid, $aid]);
                        $synced[] = ['uid' => $clientUid, 'entity' => 'stop', 'deleted' => true];
                    }
                }
            } catch (\Throwable $e) {
                $errors[] = ['uid' => $clientUid, 'entity' => $entity, 'error' => $e->getMessage()];
            }
        }
        db()->commit();
    } catch (\Throwable $e) {
        db()->rollBack();
        throw $e;
    }

    apiLog($action, $uid, $aid);
    ok([
        'synced'      => $synced,
        'errors'      => $errors,
        'server_time' => date('c'),
    ]);
}


// ── users_list ────────────────────────────────────────────────
if ($action === 'users_list') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];
    if (roleLevel($sess['role']) < 4) err('Permisos insuficientes', 403);

    $st = db()->prepare(
        'SELECT id AS user_id, username, COALESCE(name, username) AS display_name,
                email, role, active AS is_active,
                DATE_FORMAT(created_at, \'%Y-%m-%dT%H:%i:%sZ\') AS created_at
         FROM users
         WHERE account_id = ? AND id != ?
         ORDER BY role DESC, username ASC'
    );
    $st->execute([$aid, $uid]);
    $users = $st->fetchAll();

    apiLog($action, $uid, $aid);
    ok(['success' => true, 'users' => array_map(function($u) {
        return [
            'user_id'      => (int)$u['user_id'],
            'username'     => $u['username'],
            'display_name' => $u['display_name'],
            'email'        => $u['email'],
            'role'         => $u['role'],
            'is_active'    => (bool)$u['is_active'],
            'created_at'   => $u['created_at'] ?? '',
        ];
    }, $users)]);
}

// ── invite_user ───────────────────────────────────────────────
if ($action === 'invite_user') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];
    if (roleLevel($sess['role']) < 4) err('Permisos insuficientes', 403);

    $email = sanEmail($body['email'] ?? '');
    $role  = san($body['role'] ?? 'agent', 20);
    if (!$email) err('Email inválido', 400);

    $validRoles = ['admin', 'manager', 'agent', 'viewer'];
    if (!in_array($role, $validRoles, true)) err('Rol inválido', 400);
    // owner no puede invitar a otro owner
    if ($sess['role'] !== 'owner' && $role === 'admin') err('Solo el propietario puede asignar admin', 403);

    // Verificar si ya existe en la cuenta
    $st = db()->prepare('SELECT id FROM users WHERE email=? AND account_id=? LIMIT 1');
    $st->execute([$email, $aid]);
    if ($st->fetchColumn()) err('El usuario ya pertenece a esta cuenta', 409);

    // Crear invite_code reutilizable (1 uso, 7 días)
    $code    = strtoupper(bin2hex(random_bytes(4))); // 8 chars legible
    $expires = date('Y-m-d H:i:s', strtotime('+7 days'));

    db()->prepare(
        'INSERT INTO invite_codes (account_id, created_by, code, role_to_assign, uses_left, expires_at)
         VALUES (?,?,?,?,1,?)'
    )->execute([$aid, $uid, $code, $role, $expires]);

    apiLog($action, $uid, $aid);
    ok(['success' => true, 'message' => "Invitación enviada a {$email}", 'code' => $code]);
}

// ── update_role ───────────────────────────────────────────────
if ($action === 'update_role') {
    $sess         = requireAuth();
    $uid          = (int)$sess['uid'];
    $aid          = (int)$sess['account_id'];
    $targetId     = (int)($body['target_user_id'] ?? 0);
    $newRole      = san($body['role'] ?? '', 20);

    if (roleLevel($sess['role']) < 4) err('Permisos insuficientes', 403);
    if (!$targetId) err('target_user_id requerido', 400);

    $validRoles = ['admin', 'manager', 'agent', 'viewer'];
    if (!in_array($newRole, $validRoles, true)) err('Rol inválido', 400);
    if ($sess['role'] !== 'owner' && $newRole === 'admin') err('Solo el propietario puede asignar admin', 403);

    // Verificar que el target pertenece al mismo account y no es owner
    $st = db()->prepare('SELECT role FROM users WHERE id=? AND account_id=? LIMIT 1');
    $st->execute([$targetId, $aid]);
    $target = $st->fetch();
    if (!$target) err('Usuario no encontrado', 404);
    if ($target['role'] === 'owner') err('No se puede cambiar el rol del propietario', 403);
    if ($target['role'] === 'god') err('No se puede cambiar el rol de un superadmin', 403);
    if ($targetId === $uid) err('No puedes cambiar tu propio rol', 403);

    db()->prepare('UPDATE users SET role=? WHERE id=? AND account_id=?')
        ->execute([$newRole, $targetId, $aid]);

    apiLog($action, $uid, $aid);
    ok(['success' => true, 'message' => 'Rol actualizado correctamente']);
}

// ── deactivate_user ───────────────────────────────────────────
if ($action === 'deactivate_user') {
    $sess      = requireAuth();
    $uid       = (int)$sess['uid'];
    $aid       = (int)$sess['account_id'];
    $targetId  = (int)($body['target_user_id'] ?? 0);

    if (roleLevel($sess['role']) < 4) err('Permisos insuficientes', 403);
    if (!$targetId) err('target_user_id requerido', 400);
    if ($targetId === $uid) err('No puedes desactivarte a ti mismo', 403);

    $st = db()->prepare('SELECT role FROM users WHERE id=? AND account_id=? LIMIT 1');
    $st->execute([$targetId, $aid]);
    $target = $st->fetch();
    if (!$target) err('Usuario no encontrado', 404);
    if ($target['role'] === 'owner') err('No se puede desactivar al propietario', 403);
    if ($target['role'] === 'god') err('No se puede desactivar a un superadmin', 403);

    db()->prepare('UPDATE users SET active=0 WHERE id=? AND account_id=?')
        ->execute([$targetId, $aid]);

    // Invalidar todas sus sesiones activas
    db()->prepare('DELETE FROM sessions WHERE user_id=?')->execute([$targetId]);

    apiLog($action, $uid, $aid);
    ok(['success' => true, 'message' => 'Usuario desactivado correctamente']);
}

// ── update_user_prefs ─────────────────────────────────────────
if ($action === 'update_user_prefs') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];

    $prefsInput = $body['prefs'] ?? [];
    if (!is_array($prefsInput)) err('prefs debe ser un objeto JSON', 400, $action);

    $allowed = [
        'language', 'show_visit_duration', 'show_next_action',
        'show_photos', 'require_result', 'push_enabled',
        'auto_sync', 'jornada_reminder', 'jornada_reminder_hour',
    ];
    $clean = [];
    foreach ($prefsInput as $k => $v) {
        if (in_array($k, $allowed, true)) $clean[$k] = $v;
    }

    db()->prepare(
        'INSERT INTO user_prefs (user_id, prefs) VALUES (?, ?)
         ON DUPLICATE KEY UPDATE prefs = ?, updated_at = NOW()'
    )->execute([$uid, json_encode($clean), json_encode($clean)]);

    apiLog($action, $uid, $aid);
    ok(['updated' => true]);
}


// ══════════════════════════════════════════════════════════════
// GOD DASHBOARD — solo accesible por role=god
// ══════════════════════════════════════════════════════════════

// god_stats — estadísticas globales del sistema
if ($action === 'god_stats') {
    $sess = requireAuth();
    if ($sess['role'] !== 'god') err('Solo god puede acceder a estadísticas globales', 403);

    $totalAccounts = db()->query('SELECT COUNT(*) FROM accounts')->fetchColumn();
    $totalUsers    = db()->query('SELECT COUNT(*) FROM users WHERE is_active=1')->fetchColumn();
    $totalRoutes   = db()->query('SELECT COUNT(*) FROM routes WHERE deleted_at IS NULL')->fetchColumn();
    $totalStops    = db()->query('SELECT COUNT(*) FROM stops WHERE deleted_at IS NULL')->fetchColumn();
    $totalReports  = db()->query('SELECT COUNT(*) FROM kpi_values')->fetchColumn();

    // Accounts más activos (últimos 30 días)
    $topAccounts = db()->query(
        'SELECT a.id, a.name, a.type,
                COUNT(DISTINCT u.id) AS user_count,
                COUNT(DISTINCT r.id) AS route_count,
                MAX(s.updated_at)    AS last_activity
         FROM accounts a
         LEFT JOIN users  u ON u.account_id = a.id AND u.is_active = 1
         LEFT JOIN routes r ON r.account_id = a.id AND r.deleted_at IS NULL
         LEFT JOIN stops  s ON s.account_id = a.id AND s.updated_at > DATE_SUB(NOW(), INTERVAL 30 DAY)
         GROUP BY a.id
         ORDER BY last_activity DESC
         LIMIT 20'
    )->fetchAll(PDO::FETCH_ASSOC);

    // Usuarios recientes (últimos 7 días)
    $recentUsers = db()->query(
        'SELECT u.id, u.username, u.display_name, u.email, u.role, u.created_at,
                a.name AS account_name
         FROM users u
         JOIN accounts a ON a.id = u.account_id
         WHERE u.created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)
         ORDER BY u.created_at DESC
         LIMIT 20'
    )->fetchAll(PDO::FETCH_ASSOC);

    apiLog($action, $sess['uid'], $sess['account_id']);
    ok([
        'total_accounts' => (int)$totalAccounts,
        'total_users'    => (int)$totalUsers,
        'total_routes'   => (int)$totalRoutes,
        'total_stops'    => (int)$totalStops,
        'total_reports'  => (int)$totalReports,
        'top_accounts'   => $topAccounts,
        'recent_users'   => $recentUsers,
    ]);
}

// god_users_all — todos los usuarios del sistema con filtros
if ($action === 'god_users_all') {
    $sess = requireAuth();
    if ($sess['role'] !== 'god') err('Solo god puede listar todos los usuarios', 403);

    $accountId = isset($b['account_id']) ? (int)$b['account_id'] : null;
    $search    = isset($b['search'])     ? '%' . san($b['search'], 100) . '%' : null;
    $roleFilter = isset($b['role'])      ? san($b['role'], 20) : null;

    $where = ['1=1'];
    $params = [];

    if ($accountId) { $where[] = 'u.account_id = ?'; $params[] = $accountId; }
    if ($search)    { $where[] = '(u.username LIKE ? OR u.email LIKE ? OR u.display_name LIKE ?)'; $params = array_merge($params, [$search, $search, $search]); }
    if ($roleFilter){ $where[] = 'u.role = ?'; $params[] = $roleFilter; }

    $whereStr = implode(' AND ', $where);
    $stmt = db()->prepare(
        "SELECT u.id, u.username, u.display_name, u.email, u.role, u.is_active,
                u.created_at, a.id AS account_id, a.name AS account_name
         FROM users u
         JOIN accounts a ON a.id = u.account_id
         WHERE $whereStr
         ORDER BY u.created_at DESC
         LIMIT 100"
    );
    $stmt->execute($params);
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);

    apiLog($action, $sess['uid'], $sess['account_id']);
    ok(['users' => $users]);
}

// god_set_role — god puede cambiar el rol de cualquier usuario
if ($action === 'god_set_role') {
    $sess = requireAuth();
    if ($sess['role'] !== 'god') err('Solo god puede usar god_set_role', 403);

    $targetId = (int)($b['user_id'] ?? 0);
    $newRole  = san($b['role'] ?? '', 20);
    $validRoles = ['god','owner','admin','manager','agent','viewer'];

    if (!$targetId)                       err('user_id requerido');
    if (!in_array($newRole, $validRoles)) err('Rol inválido');
    if ($targetId === (int)$sess['uid'])  err('No puedes cambiar tu propio rol');

    db()->prepare('UPDATE users SET role=?, updated_at=NOW() WHERE id=?')
        ->execute([$newRole, $targetId]);

    apiLog($action, $sess['uid'], $sess['account_id']);
    ok(['updated' => true, 'user_id' => $targetId, 'new_role' => $newRole]);
}

err("Acción desconocida: {$action}", 404);



