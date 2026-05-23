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

    $roleLevel = roleLevel($sess['role']);
    $role      = $sess['role'];

    // Calcular qué user_ids puede ver el caller:
    // owner/admin/god → toda la cuenta
    // manager         → sus agentes directos + él mismo
    // agent/viewer    → solo él mismo
    if ($roleLevel >= 4) {                          // owner/admin/god
        $stR = db()->prepare(
            'SELECT uid, account_id, user_id, name, date_assigned, scheduled_dates,
                       status, notes, created_at, updated_at, deleted_at
                FROM routes WHERE account_id=? AND updated_at > ? ORDER BY updated_at ASC LIMIT 200'
        );
        $stR->execute([$aid, $since]);
        $stopsWhere = 'r.account_id=?';
        $stopsParam = $aid;
    } elseif ($role === 'manager') {                 // manager: solo sus agentes directos
        // Obtener IDs de agentes con manager_id = $uid
        $stMA = db()->prepare(
            'SELECT id FROM users WHERE account_id=? AND manager_id=? AND active=1'
        );
        $stMA->execute([$aid, $uid]);
        $agentIds = array_column($stMA->fetchAll(), 'id');
        // manager NO tiene rutas propias — solo supervisa. No añadir $uid.
        // Si no tiene agentes, devolver vacío para que no vea rutas de otros.
        $placeholders = implode(',', array_fill(0, count($agentIds), '?'));
        $stR = db()->prepare(
            "SELECT uid, account_id, user_id, name, date_assigned, scheduled_dates,
                       status, notes, created_at, updated_at, deleted_at
                FROM routes WHERE user_id IN ($placeholders) AND updated_at > ?
                ORDER BY updated_at ASC LIMIT 200"
        );
        $stR->execute([...$agentIds, $since]);
        // Sin agentes directos → manager ve lista vacía (no sus propias rutas)
        if (empty($agentIds)) {
            ok(['routes' => [], 'stops' => [], 'sessions' => []]);
        }
        $stopsWhere = "r.user_id IN ($placeholders)";
        $stopsParam = null;  // se pasa el array directamente abajo
    } else {                                         // agent/viewer
        $stR = db()->prepare(
            'SELECT uid, account_id, user_id, name, date_assigned, scheduled_dates,
                       status, notes, created_at, updated_at, deleted_at
                FROM routes WHERE user_id=? AND updated_at > ? ORDER BY updated_at ASC LIMIT 200'
        );
        $stR->execute([$uid, $since]);
        $stopsWhere = 'r.user_id=?';
        $stopsParam = $uid;
    }
    $stopsQuery = "SELECT s.id, s.uid, s.route_id, r.uid AS route_uid, s.account_id,
                s.name, s.address, s.lat, s.lng, s.order_index,
                s.external_id, s.contact_name, s.contact_phone,
                s.visit_frequency, s.priority, s.segment, s.account_status, s.opening_hours,
                s.status, s.notes, s.visited_at, s.visit_result, s.next_action, s.pdv_open, s.pdv_inactive,
                s.created_at, s.updated_at, s.deleted_at
         FROM stops s
         JOIN routes r ON r.id = s.route_id
         WHERE {$stopsWhere} AND s.updated_at > ?
         ORDER BY s.updated_at ASC LIMIT 500";
    $stS = db()->prepare($stopsQuery);
    if ($stopsParam === null) {
        // manager: $agentIds ya contiene los user_ids
        $stS->execute([...$agentIds, $since]);
    } else {
        $stS->execute([$stopsParam, $since]);
    }

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
    // Business profile de la cuenta
    $stBP = db()->prepare(
        'SELECT sector, name FROM business_profiles WHERE account_id=? LIMIT 1'
    );
    $stBP->execute([$aid]);
    $bp = $stBP->fetch() ?: null;

    // KPI definitions activos de la cuenta (isSystem=1 son catálogo global, también se restauran)
    $stKD = db()->prepare(
        'SELECT id, account_id, sector, label, type, unit, options,
                is_system, visible, required, order_index, section
         FROM kpi_definitions
         WHERE account_id IN (0, ?) AND visible=1
         ORDER BY is_system DESC, order_index ASC
         LIMIT 200'
    );
    $stKD->execute([$aid]);
    $kpiDefs = $stKD->fetchAll();

    // IDs de agentes bajo supervisión directa del caller (solo si es manager)
    $managedAgentIds = [];
    if ($role === 'manager') {
        $stMA = db()->prepare(
            'SELECT id FROM users WHERE account_id=? AND manager_id=? AND active=1'
        );
        $stMA->execute([$aid, $uid]);
        $managedAgentIds = array_column($stMA->fetchAll(), 'id');
    }

    apiLog($action, $uid, $aid);
    ok([
        'routes'             => $stR->fetchAll(),
        'stops'              => $stS->fetchAll(),
        'day_sessions'       => $stD->fetchAll(),
        'kpi_values'         => $stK->fetchAll(),
        'business_profile'   => $bp,
        'kpi_definitions'    => $kpiDefs,
        'managed_agent_ids'  => $managedAgentIds,  // [] para no-manager
        'server_time'        => date('c'),
    ]);
}

// ── batch_sync ───────────────────────────────────────────────
if ($action === 'batch_sync') {
    $sess      = requireAuth();
    $uid       = (int)$sess['uid'];
    $aid       = (int)$sess['account_id'];
    $ops       = $body['operations'] ?? [];
    $callerRole  = $sess['role'];
    $callerLevel = roleLevel($callerRole);
    // Solo owner/admin/god pueden crear o eliminar rutas y paradas
    $canCreateDelete = $callerLevel >= 4; // admin=4, owner=5, god=6

    $synced = [];
    $errors = [];

    db()->beginTransaction();
    try {
        foreach ($ops as $op) {
            $entity    = san($op['entity']    ?? '', 20);
            $operation = san($op['operation'] ?? '', 10);
            $data      = $op['data'] ?? [];
            $clientUid = san($op['uid'] ?? '', 36);

            // Bloquear create/delete de rutas y paradas para manager/agent/viewer
            if (in_array($entity, ['route', 'stop']) &&
                in_array($operation, ['create', 'delete']) &&
                !$canCreateDelete) {
                $errors[] = ['uid' => $clientUid, 'entity' => $entity,
                             'error' => 'Sin permisos para crear o eliminar ' . $entity];
                continue;
            }

            try {
                if ($entity === 'route') {
                    if ($operation === 'create' || $operation === 'update') {
                        $db = db();
                        // Comprobar si la ruta ya existe Y pertenece a esta cuenta
                        $existing = $db->prepare(
                            'SELECT id FROM routes WHERE uid=? AND account_id=? LIMIT 1'
                        );
                        $existing->execute([$clientUid, $aid]);
                        $existingId = $existing->fetchColumn();

                        if ($existingId) {
                            // Leer user_id actual para detectar reasignación
                            $stOld = $db->prepare('SELECT user_id FROM routes WHERE uid=? LIMIT 1');
                            $stOld->execute([$clientUid]);
                            $oldUserId = (int)($stOld->fetchColumn() ?: $uid);

                            // Nuevo user_id — respetar el del payload si viene de un manager/admin
                            $newUserId = isset($data['user_id']) ? (int)$data['user_id'] : $oldUserId;
                            // Verificar que newUserId pertenece a la misma cuenta
                            if ($newUserId !== $oldUserId) {
                                $stCheck = $db->prepare('SELECT id FROM users WHERE id=? AND account_id=? LIMIT 1');
                                $stCheck->execute([$newUserId, $aid]);
                                if (!$stCheck->fetchColumn()) $newUserId = $oldUserId; // fallback seguro
                            }

                            // UPDATE seguro: incluye user_id para soportar reasignación
                            $db->prepare(
                                'UPDATE routes SET
                                    user_id=?, name=?, date_assigned=?, scheduled_dates=?,
                                    status=?, notes=?, updated_at=?
                                 WHERE uid=? AND account_id=?'
                            )->execute([
                                $newUserId,
                                san($data['name'] ?? '', 255),
                                san($data['date_assigned'] ?? date('Y-m-d'), 10),
                                isset($data['scheduled_dates']) ? normalizeScheduledDates($data['scheduled_dates']) : null,
                                san($data['status'] ?? 'pending', 20),
                                san($data['notes'] ?? '', 5000) ?: null,
                                date('c'),
                                $clientUid, $aid,
                            ]);
                            $serverId = (int)$existingId;

                            // Push al nuevo asignado si cambió el propietario
                            if ($newUserId !== $oldUserId) {
                                $routeName    = san($data['name'] ?? '', 200);
                                $dateAssigned = san($data['date_assigned'] ?? '', 20);
                                pushToUser($newUserId, [
                                    'type'      => 'route_reassigned',
                                    'route_uid' => $clientUid,
                                    'title'     => 'Ruta reasignada a ti',
                                    'body'      => $routeName . ($dateAssigned ? ' — ' . $dateAssigned : ''),
                                ]);
                                // También sync al antiguo propietario para que desaparezca de su lista
                                pushToUser($oldUserId, [
                                    'type'      => 'sync',
                                    'route_uid' => $clientUid,
                                    'title'     => '',
                                    'body'      => '',
                                ]);
                            }
                        } else {
                            // INSERT solo si no existe — account_id y user_id del token
                            // user_id: respetar el del payload si lo envió un manager para un agente
                            $targetUserId = isset($data['user_id']) ? (int)$data['user_id'] : $uid;
                            // Verificar que targetUserId pertenece a la misma cuenta
                            if ($targetUserId !== $uid) {
                                $stCheck = db()->prepare('SELECT id FROM users WHERE id=? AND account_id=? LIMIT 1');
                                $stCheck->execute([$targetUserId, $aid]);
                                if (!$stCheck->fetchColumn()) $targetUserId = $uid; // fallback seguro
                            }
                            $db->prepare(
                                'INSERT INTO routes
                                    (uid, account_id, user_id, name, date_assigned, scheduled_dates, status, notes, created_at, updated_at)
                                 VALUES (?,?,?,?,?,?,?,?,?,?)'
                            )->execute([
                                $clientUid, $aid, $targetUserId,
                                san($data['name'] ?? '', 255),
                                san($data['date_assigned'] ?? date('Y-m-d'), 10),
                                isset($data['scheduled_dates']) ? normalizeScheduledDates($data['scheduled_dates']) : null,
                                san($data['status'] ?? 'pending', 20),
                                san($data['notes'] ?? '', 5000) ?: null,
                                san($data['created_at'] ?? date('c'), 30),
                                date('c'),
                            ]);
                            $serverId = (int)$db->lastInsertId() ?: null;
                        }
                        $db->prepare(
                            'INSERT INTO sync_log (account_id, user_id, entity, entity_uid, operation)
                             VALUES (?,?,?,?,?)'
                        )->execute([$aid, $uid, 'route', $clientUid, $operation]);
                        $synced[] = ['uid' => $clientUid, 'server_id' => $serverId, 'entity' => 'route'];
                        // Notificar al agente si la ruta la creó otro usuario (manager asignando)
                        if ($targetUserId !== $uid) {
                            $routeName    = san($data['name'] ?? '', 200);
                            $dateAssigned = san($data['date_assigned'] ?? '', 20);
                            pushToUser($targetUserId, [
                                'type'      => 'route_assigned',
                                'route_uid' => $clientUid,
                                'title'     => 'Nueva ruta asignada',
                                'body'      => $routeName . ($dateAssigned ? ' — ' . $dateAssigned : ''),
                            ]);
                        }

                    } elseif ($operation === 'delete') {
                        // Borrar solo si pertenece al account (manager puede borrar rutas de miembros)
                        db()->prepare(
                            'UPDATE routes SET deleted_at=NOW(), updated_at=NOW() WHERE uid=? AND account_id=?'
                        )->execute([$clientUid, $aid]);
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
                        // Comprobar si el stop ya existe Y pertenece a esta cuenta
                        $existingStop = db()->prepare(
                            'SELECT id FROM stops WHERE uid=? AND account_id=? LIMIT 1'
                        );
                        $existingStop->execute([$clientUid, $aid]);
                        $existingStopId = $existingStop->fetchColumn();

                        if ($existingStopId) {
                            // UPDATE seguro: solo si account_id coincide
                            db()->prepare(
                                'UPDATE stops SET
                                    name=?, address=?, lat=?, lng=?,
                                    order_index=?, status=?, notes=?, visited_at=?,
                                    external_id=?, contact_name=?, contact_phone=?,
                                    visit_result=?, next_action=?,
                                    pdv_open=?, pdv_inactive=?,
                                    visit_frequency=?, priority=?, segment=?,
                                    date_assigned=COALESCE(?,date_assigned),
                                    account_status=IF(?=1,\'inactive\',account_status),
                                    check_in_ts=COALESCE(?,check_in_ts),
                                    check_out_ts=COALESCE(?,check_out_ts),
                                    gps_lat_visit=COALESCE(?,gps_lat_visit),
                                    gps_lng_visit=COALESCE(?,gps_lng_visit),
                                    updated_at=?
                                 WHERE uid=? AND account_id=?'
                            )->execute([
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
                                san($data['visit_frequency'] ?? '', 20) ?: null,
                                isset($data['priority']) ? (int)$data['priority'] : 0,
                                san($data['segment'] ?? '', 50) ?: null,
                                $data['date_assigned'] ?? null,
                                isset($data['pdv_inactive']) ? (int)(bool)$data['pdv_inactive'] : 0,
                                $data['check_in_ts']   ?? null,
                                $data['check_out_ts']  ?? null,
                                isset($data['gps_lat_visit']) ? (float)$data['gps_lat_visit'] : null,
                                isset($data['gps_lng_visit']) ? (float)$data['gps_lng_visit'] : null,
                                date('c'),
                                $clientUid, $aid,
                            ]);
                        } else {
                            // INSERT solo si no existe — account_id del token
                            db()->prepare(
                                'INSERT INTO stops
                                    (uid, route_id, account_id, name, address, lat, lng,
                                     order_index, status, notes, visited_at,
                                     external_id, contact_name, contact_phone,
                                     visit_result, next_action, pdv_open, pdv_inactive,
                                     visit_frequency, priority, segment,
                                     date_assigned,
                                     created_at, updated_at)
                                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)'
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
                                san($data['visit_frequency'] ?? '', 20) ?: null,
                                isset($data['priority']) ? (int)$data['priority'] : 0,
                                san($data['segment'] ?? '', 50) ?: null,
                                $data['date_assigned'] ?? null,
                                san($data['created_at'] ?? date('c'), 30),
                                date('c'),
                            ]);
                        }
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
    // Notificar a otros dispositivos del account cuando hay datos subidos
    if ($synced > 0) {
        try { pushSyncToAccount($aid); } catch (Throwable $e) { /* silent fail */ }
    }
}


// ── users_list ────────────────────────────────────────────────
if ($action === 'users_list') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];
    $role = $sess['role'];
    // manager (level 3) puede ver sus agentes directos; admin+ ve todos
    if (roleLevel($role) < 3) err('Permisos insuficientes', 403);

    // Manager solo ve usuarios que reportan a él
    // Admin/owner/god ven todos del account
    if ($role === 'manager') {
        $st = db()->prepare(
            'SELECT u.id AS user_id, u.username,
                    COALESCE(u.name, u.username) AS display_name,
                    u.email, u.role, u.active AS is_active,
                    u.manager_id,
                    COALESCE(m.name, m.username) AS manager_name,
                    DATE_FORMAT(u.created_at, \'%Y-%m-%dT%H:%i:%sZ\') AS created_at
             FROM users u
             LEFT JOIN users m ON m.id = u.manager_id AND m.account_id = u.account_id
             WHERE u.account_id = ? AND u.manager_id = ? AND u.id != ?
             ORDER BY u.role DESC, u.username ASC'
        );
        $st->execute([$aid, $uid, $uid]);
    } else {
        $st = db()->prepare(
            'SELECT u.id AS user_id, u.username,
                    COALESCE(u.name, u.username) AS display_name,
                    u.email, u.role, u.active AS is_active,
                    u.manager_id,
                    COALESCE(m.name, m.username) AS manager_name,
                    DATE_FORMAT(u.created_at, \'%Y-%m-%dT%H:%i:%sZ\') AS created_at
             FROM users u
             LEFT JOIN users m ON m.id = u.manager_id AND m.account_id = u.account_id
             WHERE u.account_id = ? AND u.id != ?
             ORDER BY u.role DESC, u.username ASC'
        );
        $st->execute([$aid, $uid]);
    }
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
            'manager_id'   => $u['manager_id'] ? (int)$u['manager_id'] : null,
            'manager_name' => $u['manager_name'] ?? null,
            'created_at'   => $u['created_at'] ?? '',
        ];
    }, $users)]);
}

// ── invite_list — listar códigos de invitación activos ────────
if ($action === 'invite_list') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];
    if (roleLevel($sess['role']) < 4) err('Permisos insuficientes', 403);

    $st = db()->prepare(
        'SELECT id, code, role_to_assign, uses_left,
                DATE_FORMAT(expires_at, \'%Y-%m-%dT%H:%i:%sZ\') AS expires_at,
                DATE_FORMAT(created_at, \'%Y-%m-%dT%H:%i:%sZ\') AS created_at
         FROM invite_codes
         WHERE account_id = ? AND expires_at > NOW() AND uses_left > 0
         ORDER BY created_at DESC'
    );
    $st->execute([$aid]);
    $invites = $st->fetchAll();

    apiLog($action, $uid, $aid);
    ok(['success' => true, 'invites' => array_map(function($i) {
        return [
            'id'             => (int)$i['id'],
            'code'           => $i['code'],
            'role_to_assign' => $i['role_to_assign'],
            'uses_left'      => (int)$i['uses_left'],
            'expires_at'     => $i['expires_at'],
            'created_at'     => $i['created_at'],
        ];
    }, $invites)]);
}

// ── invite_delete — eliminar invitación ───────────────────────
if ($action === 'invite_delete') {
    $sess     = requireAuth();
    $uid      = (int)$sess['uid'];
    $aid      = (int)$sess['account_id'];
    $inviteId = (int)($body['invite_id'] ?? 0);
    if (roleLevel($sess['role']) < 4) err('Permisos insuficientes', 403);
    if (!$inviteId) err('invite_id requerido', 400);

    $deleted = db()->prepare(
        'DELETE FROM invite_codes WHERE id=? AND account_id=?'
    )->execute([$inviteId, $aid]);

    apiLog($action, $uid, $aid);
    ok(['success' => true, 'message' => 'Invitación eliminada']);
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

// ── assign_manager — asignar/quitar supervisor a un usuario ─
if ($action === 'assign_manager') {
    $sess      = requireAuth();
    $uid       = (int)$sess['uid'];
    $aid       = (int)$sess['account_id'];
    $myRole    = $sess['role'];
    $myLevel   = roleLevel($myRole);

    if ($myLevel < 3) err('Permisos insuficientes — requiere manager o superior', 403);

    $targetId  = (int)($body['target_user_id'] ?? 0);
    $managerId = isset($body['manager_id']) ? (int)$body['manager_id'] : null;

    if (!$targetId) err('target_user_id requerido', 400);
    if ($targetId === $uid) err('No puedes asignarte a ti mismo como subordinado', 400);

    // Verificar target en misma cuenta
    $stT = db()->prepare('SELECT id, role FROM users WHERE id=? AND account_id=? LIMIT 1');
    $stT->execute([$targetId, $aid]);
    $target = $stT->fetch();
    if (!$target) err('Usuario destino no encontrado en tu cuenta', 404);

    // El invocante solo puede asignar como supervisor a alguien de nivel INFERIOR
    if (roleLevel($target['role']) >= $myLevel && $myRole !== 'owner' && $myRole !== 'god') {
        err('Solo puedes asignar supervisores a usuarios de menor rango que tú', 403);
    }

    // ── Validar que el supervisor es el rol inmediatamente superior ──
    // Jerarquía fija: owner > admin > manager > agent/viewer
    // owner puede supervisar a todos
    // admin puede supervisar a: manager, agent, viewer
    // manager puede supervisar a: agent, viewer
    // agent/viewer no pueden supervisar a nadie
    $validSupervisorRoles = [
        'admin'   => ['owner'],                           // admin solo reporta a owner
        'manager' => ['admin', 'owner'],                  // manager reporta a admin (u owner directo)
        'agent'   => ['manager', 'admin', 'owner'],       // agent reporta a manager (o superior)
        'viewer'  => ['manager', 'admin', 'owner'],       // viewer igual que agent
    ];
    $targetRoleKey = $target['role'];

    if ($managerId !== null) {
        if ($managerId === $targetId) err('Un usuario no puede ser su propio supervisor', 400);
        $stM = db()->prepare('SELECT id, role FROM users WHERE id=? AND account_id=? AND active=1 LIMIT 1');
        $stM->execute([$managerId, $aid]);
        $manager = $stM->fetch();
        if (!$manager) err('Supervisor no encontrado en tu cuenta', 404);

        // Verificar que el supervisor es un rol válido para este subordinado
        $allowedSupervisors = $validSupervisorRoles[$targetRoleKey] ?? [];
        if (!in_array($manager['role'], $allowedSupervisors, true)) {
            $allowed = implode(' o ', $allowedSupervisors);
            err("Un {$targetRoleKey} solo puede reportar a: {$allowed}", 400);
        }

        // El caller debe poder gestionar al target (nivel inferior al propio)
        // EXCEPCIÓN: owner puede asignar a cualquiera
        if ($myRole !== 'owner' && $myRole !== 'god') {
            if (roleLevel($manager['role']) >= $myLevel) {
                err('No puedes asignar como supervisor a alguien con igual o mayor autoridad que tú', 403);
            }
        }
    }

    // Detectar ciclos: el nuevo manager no puede ser subordinado (directo o transitivo) del target
    if ($managerId !== null) {
        $check = $managerId;
        $hops  = 0;
        while ($check !== null && $hops < 10) {
            if ($check === $targetId) err('Asignación circular detectada — el supervisor es subordinado del usuario', 400);
            $stC = db()->prepare('SELECT manager_id FROM users WHERE id=? AND account_id=? LIMIT 1');
            $stC->execute([$check, $aid]);
            $row   = $stC->fetch();
            $check = $row ? ($row['manager_id'] ? (int)$row['manager_id'] : null) : null;
            $hops++;
        }
    }

    db()->prepare('UPDATE users SET manager_id=? WHERE id=? AND account_id=?')
       ->execute([$managerId ?: null, $targetId, $aid]);

    $msg = $managerId ? 'Supervisor asignado correctamente' : 'Supervisor eliminado correctamente';
    apiLog($action, $uid, $aid);
    ok(['success' => true, 'message' => $msg]);
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

// ── reactivate_user ──────────────────────────────────────────
if ($action === 'reactivate_user') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];
    if (roleLevel($sess['role']) < 4) err('Permisos insuficientes', 403);

    $targetId = (int)($body['target_user_id'] ?? 0);
    if (!$targetId) err('target_user_id requerido', 400);

    // Verificar que el target pertenece a la misma cuenta
    $st = db()->prepare('SELECT id, role FROM users WHERE id=? AND account_id=?');
    $st->execute([$targetId, $aid]);
    $target = $st->fetch();
    if (!$target) err('Usuario no encontrado en tu cuenta', 404);

    // No puede reactivar a alguien de mayor rango
    if (roleLevel($target['role']) >= roleLevel($sess['role'])) {
        err('No puedes reactivar a un usuario con rol igual o superior', 403);
    }

    db()->prepare('UPDATE users SET active=1 WHERE id=? AND account_id=?')
       ->execute([$targetId, $aid]);

    apiLog($action, $uid, $aid);
    ok(['success' => true, 'message' => 'Usuario reactivado correctamente']);
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
        'vacation_days',  // JSON array de fechas ISO ["2026-05-12", ...]
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
// FILE UPLOAD — fotos de visita
// ══════════════════════════════════════════════════════════════

if ($action === 'file_upload') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];

    $stopUid  = san($_POST['stop_uid']  ?? '', 36);
    $photoUid = san($_POST['photo_uid'] ?? '', 36);

    if (!$stopUid || !$photoUid) err('stop_uid y photo_uid requeridos', 400, $action);
    if (empty($_FILES['file'])) err('Archivo no recibido', 400, $action);

    $file  = $_FILES['file'];
    if ($file['error'] !== UPLOAD_ERR_OK) err('Error en la subida del archivo', 400, $action);

    // Validar tipo MIME
    $allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    $finfo   = new finfo(FILEINFO_MIME_TYPE);
    $mime    = $finfo->file($file['tmp_name']);
    if (!in_array($mime, $allowed, true)) err('Tipo de archivo no permitido', 400, $action);

    // Limitar tamaño: 10 MB
    if ($file['size'] > 10 * 1024 * 1024) err('Archivo demasiado grande (máx 10 MB)', 400, $action);

    // Directorio de destino: uploads/photos/{account_id}/{año}/{mes}/
    $ext     = $mime === 'image/png' ? 'png' : 'jpg';
    $year    = date('Y');
    $month   = date('m');
    $dir     = __DIR__ . "/uploads/photos/{$aid}/{$year}/{$month}";
    if (!is_dir($dir)) mkdir($dir, 0755, true);

    $filename = "photo_{$photoUid}.{$ext}";
    $filepath = "{$dir}/{$filename}";
    $fileurl  = "/rutasproapk/uploads/photos/{$aid}/{$year}/{$month}/{$filename}";

    if (!move_uploaded_file($file['tmp_name'], $filepath)) {
        err('Error al guardar el archivo en disco', 500, $action);
    }

    // Guardar en BD — ON DUPLICATE KEY ignora re-uploads del mismo photo_uid
    db()->prepare(
        'INSERT INTO visit_photos (account_id, user_id, stop_uid, photo_uid, file_path, file_url)
         VALUES (?, ?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE file_path=VALUES(file_path), file_url=VALUES(file_url)'
    )->execute([$aid, $uid, $stopUid, $photoUid, $filepath, $fileurl]);

    apiLog($action, $uid, $aid);
    ok(['url' => $fileurl, 'path' => $filepath]);
}

// ══════════════════════════════════════════════════════════════
// PUSH REGISTER — actualiza fcm_token cuando Firebase lo rota
// ══════════════════════════════════════════════════════════════

if ($action === 'push_register') {
    $sess     = requireAuth();
    $uid      = (int)$sess['uid'];
    $body     = $body ?? [];

    $fcmToken  = san($body['fcm_token']  ?? '', 4096);
    $deviceId  = san($body['device_id']  ?? '', 64);
    $platform  = san($body['platform']   ?? 'android', 16);
    $appVersion= san($body['app_version'] ?? '', 32);

    if (!$fcmToken || !$deviceId) err('fcm_token y device_id son obligatorios', 400, $action);

    // Actualizar fcm_token en la sesión activa del dispositivo
    // Si el device_id no tiene sesión activa (no debería ocurrir), no hace nada
    $updated = db()->prepare(
        'UPDATE sessions
         SET fcm_token    = ?,
             app_version  = COALESCE(?, app_version),
             last_used_at = NOW()
         WHERE user_id   = ?
           AND device_id = ?
           AND expires_at > NOW()'
    )->execute([$fcmToken, $appVersion ?: null, $uid, $deviceId]);

    apiLog($action, $uid, (int)$sess['account_id']);
    ok(['updated' => $updated]);
}

// ══════════════════════════════════════════════════════════════
// ACCOUNT CONFIG SAVE — actualiza configuración de la cuenta
// Solo owner/admin pueden modificar su propia cuenta.
// ══════════════════════════════════════════════════════════════

if ($action === 'account_config_save') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];
    $role = $sess['role'] ?? 'agent';

    if (!in_array($role, ['owner', 'god'], true)) {
        err('Sin permisos — solo el propietario puede cambiar la configuración de la empresa', 403, $action);
    }

    $allowed = ['name', 'plus_config', 'form_config', 'ai_settings'];
    $updates = [];
    $params  = [];

    if (isset($body['name'])) {
        $name = san($body['name'], 100);
        if (strlen($name) < 2) err('El nombre debe tener al menos 2 caracteres', 400, $action);
        $updates[] = 'name = ?';
        $params[]  = $name;
    }

    // JSON fields — almacenados como texto, validados como JSON
    foreach (['plus_config', 'form_config', 'ai_settings'] as $field) {
        if (isset($body[$field])) {
            $val = $body[$field];
            // Aceptar null (borrar) o array/objeto
            if ($val === null) {
                $updates[] = "$field = NULL";
            } else {
                if (!is_array($val) && !is_object($val)) {
                    err("$field debe ser un objeto JSON o null", 400, $action);
                }
                $updates[] = "$field = ?";
                $params[]  = json_encode($val, JSON_UNESCAPED_UNICODE);
            }
        }
    }

    if (empty($updates)) err('No hay campos que actualizar', 400, $action);

    $params[] = $aid;
    db()->prepare(
        'UPDATE accounts SET ' . implode(', ', $updates) . ', updated_at = NOW() WHERE id = ?'
    )->execute($params);

    // Devolver la cuenta actualizada
    $st = db()->prepare(
        'SELECT id, name, type, slug, plan, plus_config, form_config, ai_settings
         FROM accounts WHERE id = ?'
    );
    $st->execute([$aid]);
    $row = $st->fetch();

    apiLog($action, $uid, $aid);
    ok([
        'account' => [
            'id'          => (int)$row['id'],
            'name'        => $row['name'],
            'type'        => $row['type'],
            'slug'        => $row['slug'],
            'plan'        => $row['plan'],
            'plus_config' => $row['plus_config']  ? json_decode($row['plus_config'],  true) : null,
            'form_config' => $row['form_config']  ? json_decode($row['form_config'],  true) : null,
            'ai_settings' => $row['ai_settings']  ? json_decode($row['ai_settings'],  true) : null,
        ],
    ]);
}

// ══════════════════════════════════════════════════════════════
// STATS MONTH — agregados del mes para manager/owner
// Devuelve métricas de todos los agentes de la cuenta para el mes dado.
// Solo accesible para roles manager, admin, owner, god.
// ══════════════════════════════════════════════════════════════

if ($action === 'stats_month') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];
    $role = $sess['role'] ?? 'agent';

    // Solo manager o superior puede ver datos de todo el equipo
    $teamRoles = ['manager', 'admin', 'owner', 'god'];
    if (!in_array($role, $teamRoles, true)) err('Sin permisos', 403, $action);

    $month = san($_GET['month'] ?? date('Y-m'), 20); // formato YYYY-MM
    if (!preg_match('/^\d{4}-\d{2}$/', $month)) err('Formato de mes inválido (YYYY-MM)', 400, $action);

    $monthStart = $month . '-01';
    $monthEnd   = date('Y-m-t', strtotime($monthStart)); // último día del mes

    // ── Filtro por agente específico (para KpisScreen selector de agente) ─
    $targetUid = isset($_GET['target_user_id']) ? (int)$_GET['target_user_id'] : null;
    if ($targetUid) {
        // Validar que el caller puede ver este agente
        if ($role === 'manager') {
            $chk = db()->prepare('SELECT id FROM users WHERE id=? AND manager_id=? AND account_id=? LIMIT 1');
            $chk->execute([$targetUid, $uid, $aid]);
            if (!$chk->fetch()) err('No tienes acceso a este agente', 403);
        } elseif (roleLevel($role) < 4) {
            err('Sin permisos para ver stats de otro usuario', 403);
        }
        // Verificar que el target pertenece a la cuenta
        $chk2 = db()->prepare('SELECT id FROM users WHERE id=? AND account_id=? LIMIT 1');
        $chk2->execute([$targetUid, $aid]);
        if (!$chk2->fetch()) err('Usuario no encontrado', 404);
    }
    // userFilter: si hay target_user_id filtramos por ese agente, si no por la cuenta
    $userFilter     = $targetUid ? 'r.user_id = ?' : 'r.account_id = ?';
    $userFilterVal  = $targetUid ?? $aid;

    // ── Totales de visitas del mes ────────────────────────────
    $stVisits = db()->prepare(
        'SELECT
            COUNT(*)                                        AS total_stops,
            SUM(s.status = "done")                         AS done_stops,
            SUM(s.status = "skipped")                      AS skipped_stops,
            SUM(s.status = "pending")                      AS pending_stops,
            SUM(s.visit_result = "contactado")             AS contacted,
            SUM(s.visit_result = "no_estaba")              AS not_home,
            SUM(s.visit_result = "volvemos")               AS return_visit,
            SUM(s.visit_result = "rechazado")              AS rejected,
            COUNT(DISTINCT r.user_id)                      AS active_agents,
            COUNT(DISTINCT r.id)                           AS total_routes,
            SUM(r.status = "done")                         AS done_routes
         FROM stops s
         JOIN routes r ON r.uid = s.route_uid
         WHERE $userFilter
           AND r.date_assigned BETWEEN ? AND ?
           AND s.deleted_at IS NULL'
    );
    $stVisits->execute([$userFilterVal, $monthStart, $monthEnd]);
    $visits = $stVisits->fetch();

    // ── KPI values agregados del mes (número + boolean) ───────
    $stKpis = db()->prepare(
        'SELECT
            kv.kpi_id,
            kd.label,
            kd.type,
            kd.unit,
            kd.section,
            COUNT(kv.id)                                        AS count_entries,
            SUM(CASE WHEN kd.type = "number"
                     THEN CAST(kv.value_text AS DECIMAL(15,4))
                     ELSE 0 END)                                AS total_value,
            SUM(CASE WHEN kd.type = "boolean" AND kv.value_text = "true"
                     THEN 1 ELSE 0 END)                        AS true_count
         FROM kpi_values kv
         JOIN kpi_definitions kd ON kd.id = kv.kpi_id
         JOIN stops s            ON s.uid  = kv.stop_uid
         JOIN routes r           ON r.uid  = s.route_uid
         WHERE r.date_assigned BETWEEN ? AND ?
           AND kd.type IN ("number", "boolean")
           AND s.deleted_at IS NULL
         GROUP BY kv.kpi_id, kd.label, kd.type, kd.unit, kd.section
         ORDER BY kd.section ASC, kd.label ASC'
    );
    $stKpis->execute([$userFilterVal, $monthStart, $monthEnd]);
    $kpiAggregates = $stKpis->fetchAll();

    // ── Por agente (para desglose opcional) ──────────────────
    // Manager solo ve los agentes que tiene asignados directamente
    $isManager = $role === 'manager';
    if ($isManager) {
        $stAgents = db()->prepare(
            'SELECT
                u.id           AS user_id,
                u.name,
                u.username,
                COUNT(s.id)                        AS total_stops,
                SUM(s.status = "done")             AS done_stops,
                SUM(s.visit_result = "contactado") AS contacted
             FROM stops s
             JOIN routes r ON r.uid = s.route_uid
         WHERE $userFilter AND
             JOIN users u  ON u.id = r.user_id AND u.manager_id = ?
             WHERE r.date_assigned BETWEEN ? AND ?
               AND s.deleted_at IS NULL
             GROUP BY u.id, u.name, u.username
             ORDER BY done_stops DESC'
        );
        $stAgents->execute([$aid, $uid, $monthStart, $monthEnd]);
    } else {
        $stAgents = db()->prepare(
            'SELECT
                u.id           AS user_id,
                u.name,
                u.username,
                COUNT(s.id)                        AS total_stops,
                SUM(s.status = "done")             AS done_stops,
                SUM(s.visit_result = "contactado") AS contacted
             FROM stops s
             JOIN routes r ON r.uid = s.route_uid
         WHERE $userFilter AND
             JOIN users u  ON u.id = r.user_id
             WHERE r.date_assigned BETWEEN ? AND ?
               AND s.deleted_at IS NULL
             GROUP BY u.id, u.name, u.username
             ORDER BY done_stops DESC'
        );
        $stAgents->execute([$userFilterVal, $monthStart, $monthEnd]);
    }
    $agents = $stAgents->fetchAll();

    apiLog($action, $uid, $aid);
    ok([
        'month'          => $month,
        'visits'         => $visits,
        'kpi_aggregates' => $kpiAggregates,
        'agents'         => $agents,
    ]);
}

// ══════════════════════════════════════════════════════════════
// GOD DASHBOARD
// ══════════════════════════════════════════════════════════════

if ($action === 'god_stats') {
    $sess = requireAuth();
    if ($sess['role'] !== 'god') err('Solo god puede acceder', 403);
    $ta = (int)db()->query('SELECT COUNT(*) FROM accounts')->fetchColumn();
    $tu = (int)db()->query('SELECT COUNT(*) FROM users WHERE active=1')->fetchColumn();
    $tr = (int)db()->query('SELECT COUNT(*) FROM routes WHERE deleted_at IS NULL')->fetchColumn();
    $ts = (int)db()->query('SELECT COUNT(*) FROM stops WHERE deleted_at IS NULL')->fetchColumn();
    $tk = (int)db()->query('SELECT COUNT(*) FROM kpi_values')->fetchColumn();
    $topAccounts = db()->query(
        'SELECT a.id, a.name, a.type, a.plan,
                COUNT(DISTINCT u.id) AS user_count,
                COUNT(DISTINCT r.id) AS route_count,
                MAX(s.updated_at) AS last_activity
         FROM accounts a
         LEFT JOIN users u ON u.account_id=a.id AND u.active=1
         LEFT JOIN routes r ON r.account_id=a.id AND r.deleted_at IS NULL
         LEFT JOIN stops s ON s.account_id=a.id AND s.updated_at > DATE_SUB(NOW(),INTERVAL 30 DAY)
         GROUP BY a.id ORDER BY last_activity DESC LIMIT 20'
    )->fetchAll(PDO::FETCH_ASSOC);
    $recentUsers = db()->query(
        'SELECT u.id, u.username, u.name AS display_name, u.email, u.role, u.created_at,
                a.name AS account_name
         FROM users u JOIN accounts a ON a.id=u.account_id
         WHERE u.created_at > DATE_SUB(NOW(),INTERVAL 7 DAY)
         ORDER BY u.created_at DESC LIMIT 20'
    )->fetchAll(PDO::FETCH_ASSOC);
    apiLog($action, $sess['uid'], $sess['account_id']);
    ok(['total_accounts'=>$ta,'total_users'=>$tu,'total_routes'=>$tr,
        'total_stops'=>$ts,'total_reports'=>$tk,
        'top_accounts'=>$topAccounts,'recent_users'=>$recentUsers]);
}

if ($action === 'god_users_all') {
    $sess = requireAuth();
    if ($sess['role'] !== 'god') err('Solo god puede listar todos los usuarios', 403);
    $accountId  = isset($body['account_id']) ? (int)$body['account_id'] : null;
    $search     = isset($body['search'])     ? '%'.san($body['search'],100).'%' : null;
    $roleFilter = isset($body['role'])       ? san($body['role'],20) : null;
    $where = ['1=1']; $params = [];
    if ($accountId)  { $where[] = 'u.account_id=?';          $params[] = $accountId; }
    if ($search)     { $where[] = '(u.username LIKE ? OR u.email LIKE ? OR u.name LIKE ?)'; $params = array_merge($params,[$search,$search,$search]); }
    if ($roleFilter) { $where[] = 'u.role=?';                 $params[] = $roleFilter; }
    $stmt = db()->prepare(
        "SELECT u.id, u.username, u.name AS display_name, u.email, u.role,
                u.active AS is_active, u.created_at, u.last_login_at, u.avatar_url,
                a.id AS account_id, a.name AS account_name
         FROM users u JOIN accounts a ON a.id=u.account_id
         WHERE ".implode(' AND ',$where)." ORDER BY u.created_at DESC LIMIT 100"
    );
    $stmt->execute($params);
    apiLog($action, $sess['uid'], $sess['account_id']);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    // Castear tinyint a bool para que Kotlin/Moshi no falle
    $rows = array_map(function($u) {
        $u['is_active'] = (bool)$u['is_active'];
        return $u;
    }, $rows);
    ok(['users' => $rows]);
}

// ── Helper: normalizar scheduled_dates (acepta CSV o JSON array) ──────────
function normalizeScheduledDates(?string $raw): ?string {
    if ($raw === null || $raw === '') return null;
    $raw = trim($raw);
    // Si es JSON array → convertir a CSV
    $decoded = json_decode($raw, true);
    if (is_array($decoded)) {
        $csv = implode(',', array_filter(array_map('trim', $decoded)));
        return $csv ?: null;
    }
    // Ya es CSV → devolver tal cual (limpiando espacios)
    $parts = array_filter(array_map('trim', explode(',', $raw)));
    return $parts ? implode(',', $parts) : null;
}

// ── Helper: push FCM a un usuario específico (por userId) ─────────────────
function pushToUser(int $targetUserId, array $data): void {
    $serverKey = defined('FCM_SERVER_KEY') ? FCM_SERVER_KEY : ($_ENV['FCM_SERVER_KEY'] ?? null);
    if (!$serverKey) return;

    // Obtener tokens activos del usuario destino
    $st = db()->prepare(
        "SELECT DISTINCT fcm_token FROM sessions
         WHERE user_id = ? AND fcm_token IS NOT NULL AND fcm_token != ''
           AND expires_at > NOW()
         LIMIT 10"
    );
    $st->execute([$targetUserId]);
    $tokens = $st->fetchAll(PDO::FETCH_COLUMN);
    if (empty($tokens)) return;

    $payload = [
        'registration_ids' => $tokens,
        'data'             => $data,
        'notification'     => [
            'title' => $data['title'] ?? 'RutasApp',
            'body'  => $data['body']  ?? '',
        ],
        'priority' => 'high',
        'android'  => ['priority' => 'high'],
    ];

    $ch = curl_init('https://fcm.googleapis.com/fcm/send');
    curl_setopt_array($ch, [
        CURLOPT_POST           => true,
        CURLOPT_HTTPHEADER     => [
            'Content-Type: application/json',
            'Authorization: key=' . $serverKey,
        ],
        CURLOPT_POSTFIELDS     => json_encode($payload),
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT        => 5,
    ]);
    curl_exec($ch);
    curl_close($ch);
}

// ── Helper: push FCM sync a todos los tokens del account ─────────────────
function pushSyncToAccount(int $accountId): void {
    $serverKey = defined('FCM_SERVER_KEY') ? FCM_SERVER_KEY : ($_ENV['FCM_SERVER_KEY'] ?? null);
    if (!$serverKey) return;
    // El token FCM está en sessions.fcm_token (no tabla separada)
    $st = db()->prepare(
        "SELECT DISTINCT s.fcm_token FROM sessions s
         JOIN users u ON s.user_id = u.id
         WHERE u.account_id = ? AND s.fcm_token IS NOT NULL AND s.fcm_token != ''
           AND s.expires_at > NOW()
         LIMIT 500"
    );
    $st->execute([$accountId]);
    $tokens = $st->fetchAll(PDO::FETCH_COLUMN);
    if (empty($tokens)) return;
    $ch = curl_init('https://fcm.googleapis.com/fcm/send');
    curl_setopt_array($ch, [
        CURLOPT_POST           => true,
        CURLOPT_HTTPHEADER     => ['Content-Type: application/json', 'Authorization: key=' . $serverKey],
        CURLOPT_POSTFIELDS     => json_encode(['registration_ids' => $tokens, 'data' => ['type' => 'sync'], 'priority' => 'high']),
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT        => 5,
    ]);
    curl_exec($ch);
    curl_close($ch);
}

if ($action === 'god_set_role') {
    $sess = requireAuth();
    if ($sess['role'] !== 'god') err('Solo god puede usar god_set_role', 403);
    $targetId = (int)($body['user_id'] ?? 0);
    $newRole  = san($body['role'] ?? '', 20);
    if (!$targetId) err('user_id requerido');
    if (!in_array($newRole,['god','owner','admin','manager','agent','viewer'])) err('Rol invalido');
    if ($targetId === (int)$sess['uid']) err('No puedes cambiar tu propio rol');
    db()->prepare('UPDATE users SET role=?, updated_at=NOW() WHERE id=?')->execute([$newRole,$targetId]);
    apiLog($action, $sess['uid'], $sess['account_id']);
    ok(['updated'=>true,'user_id'=>$targetId,'new_role'=>$newRole]);
}

if ($action === 'clear_routes') {
    $sess = requireAuth();
    // Solo owner/god pueden borrar todo el contenido de la cuenta
    if (roleLevel($sess['role']) < 5) err('Forbidden', 403);
    $aid = (int)$sess['account_id'];
    // Borrar en orden correcto por FK: kpi_values → stops → routes → sync_queue
    db()->prepare('DELETE kv FROM kpi_values kv
        JOIN stops s ON s.uid = kv.stop_uid
        JOIN routes r ON r.uid = s.route_uid
        WHERE r.account_id = ?')->execute([$aid]);
    db()->prepare('DELETE s FROM stops s
        JOIN routes r ON r.uid = s.route_uid
        WHERE r.account_id = ?')->execute([$aid]);
    db()->prepare('DELETE FROM routes WHERE account_id = ?')->execute([$aid]);
    // sync_log registra operaciones — no hay sync_queue en este esquema
    // Los registros de sync_log de esta cuenta se limpian también
    db()->prepare('DELETE FROM sync_log WHERE account_id = ?')->execute([$aid]);
    apiLog($action, $sess['uid'], $aid);
    ok(['cleared' => true]);
}

if ($action === 'assign_route') {
    $sess     = requireAuth();
    $callerId = (int)$sess['uid'];
    $callerRole = $sess['role'];
    $aid      = (int)$sess['account_id'];

    // Mínimo manager para reasignar
    if (roleLevel($callerRole) < 3) err('Sin permisos para reasignar rutas', 403);

    $routeUid  = san($body['route_uid']  ?? '', 50);
    $newUserId = (int)($body['new_user_id'] ?? 0);
    if (!$routeUid)   err('route_uid requerido');
    if (!$newUserId)  err('new_user_id requerido');

    // Verificar que la ruta pertenece a la cuenta
    $route = db()->prepare('SELECT id, user_id, name FROM routes WHERE uid=? AND account_id=? AND deleted_at IS NULL LIMIT 1');
    $route->execute([$routeUid, $aid]);
    $routeRow = $route->fetch();
    if (!$routeRow) err('Ruta no encontrada', 404);

    // Verificar que el destinatario existe y el caller puede asignarle
    $target = db()->prepare('SELECT id, name, role, manager_id FROM users WHERE id=? AND account_id=? AND active=1 LIMIT 1');
    $target->execute([$newUserId, $aid]);
    $targetRow = $target->fetch();
    if (!$targetRow) err('Usuario destino no encontrado', 404);

    // Regla: manager solo puede asignar a sus agentes directos
    if ($callerRole === 'manager') {
        if ((int)$targetRow['manager_id'] !== $callerId) {
            err('Solo puedes asignar rutas a tus agentes directos', 403);
        }
    }
    // admin puede asignar a cualquiera de la cuenta excepto owner/god
    if ($callerRole === 'admin' && in_array($targetRow['role'], ['owner', 'god'])) {
        err('No puedes asignar rutas a owner o god', 403);
    }

    // Actualizar user_id en la ruta
    db()->prepare('UPDATE routes SET user_id=?, updated_at=NOW() WHERE uid=? AND account_id=?')
        ->execute([$newUserId, $routeUid, $aid]);

    // Actualizar también los stops de esa ruta para que el nuevo agente los vea
    db()->prepare('UPDATE stops s JOIN routes r ON r.uid=s.route_uid SET s.updated_at=NOW() WHERE r.uid=? AND r.account_id=?')
        ->execute([$routeUid, $aid]);

    // FCM push al nuevo agente
    $fcmToken = db()->prepare('SELECT fcm_token FROM users WHERE id=? AND fcm_token IS NOT NULL LIMIT 1');
    $fcmToken->execute([$newUserId]);
    $tokenRow = $fcmToken->fetch();
    if ($tokenRow && $tokenRow['fcm_token']) {
        sendFcmNotification(
            $tokenRow['fcm_token'],
            'Nueva ruta asignada',
            'Se te ha asignado la ruta: ' . $routeRow['name'],
            ['type' => 'route_assigned', 'route_uid' => $routeUid]
        );
    }

    apiLog($action, $callerId, $aid);
    ok(['assigned' => true, 'route_uid' => $routeUid, 'new_user_id' => $newUserId]);
}

// ─── S20: team_overview ────────────────────────────────────────────────────
if ($action === 'team_overview') {
    $sess = requireAuth();
    $uid  = (int)$sess['uid'];
    $aid  = (int)$sess['account_id'];
    $role = $sess['role'];

    if (roleLevel($role) < 3) err('Sin permisos', 403);

    $today = date('Y-m-d');
    $month = date('Y-m');

    // Obtener usuarios según jerarquía
    if ($role === 'manager') {
        // Manager: solo sus agentes directos
        $stU = db()->prepare(
            'SELECT id, name, username, role, avatar_url FROM users
             WHERE account_id=? AND manager_id=? AND active=1 AND role="agent"
             ORDER BY name ASC'
        );
        $stU->execute([$aid, $uid]);
    } elseif ($role === 'admin') {
        // Admin: sus managers directos + los agentes de esos managers
        // Primero obtener sus managers directos
        $stMgr = db()->prepare(
            'SELECT id FROM users WHERE account_id=? AND manager_id=? AND active=1 AND role="manager"'
        );
        $stMgr->execute([$aid, $uid]);
        $directManagers = array_column($stMgr->fetchAll(), 'id');

        if (empty($directManagers)) {
            // Sin managers directos → mostrar todos los agentes y managers de la cuenta
            $stU = db()->prepare(
                'SELECT id, name, username, role, avatar_url FROM users
                 WHERE account_id=? AND active=1 AND role IN ("agent","manager")
                 ORDER BY role DESC, name ASC'
            );
            $stU->execute([$aid]);
        } else {
            // Sus managers + los agentes de esos managers
            $mgrPh = implode(',', array_fill(0, count($directManagers), '?'));
            $stU = db()->prepare(
                "SELECT id, name, username, role, avatar_url FROM users
                 WHERE account_id=? AND active=1
                   AND (manager_id=? AND role='manager'
                        OR (manager_id IN ($mgrPh) AND role='agent'))
                 ORDER BY role DESC, name ASC"
            );
            $stU->execute(array_merge([$aid, $uid], $directManagers));
        }
    } elseif (roleLevel($role) >= 5) {
        // Owner/god: toda la cadena (agents + managers)
        $stU = db()->prepare(
            'SELECT id, name, username, role, avatar_url FROM users
             WHERE account_id=? AND active=1 AND role IN ("agent","manager","admin")
             ORDER BY role DESC, name ASC'
        );
        $stU->execute([$aid]);
    } else {
        ok(['agents' => []]);
    }
    $agents = $stU->fetchAll();
    if (empty($agents)) { ok(['agents' => []]); }

    $agentIds = array_column($agents, 'id');
    if (empty($agentIds)) { ok(['agents' => []]); }  // guard adicional

    $ph = implode(',', array_fill(0, count($agentIds), '?'));

    // Jornada activa de hoy por agente
    $stJ = db()->prepare(
        "SELECT user_id, state, elapsed_ms, distance_km, last_lat, last_lng, updated_at
         FROM day_sessions WHERE user_id IN ($ph) AND date_str=? ORDER BY updated_at DESC"
    );
    $paramsJ = array_merge($agentIds, [$today]);
    $stJ->execute($paramsJ);
    $jornadas = [];
    foreach ($stJ->fetchAll() as $j) {
        $jornadas[$j['user_id']] = $j;
    }

    // Stops de hoy por agente
    $stS = db()->prepare(
        "SELECT r.user_id,
                COUNT(s.id)                         AS total,
                SUM(s.status='done')                AS done,
                SUM(s.status='skipped')             AS skipped,
                SUM(s.visit_result='contactado')    AS contacted
         FROM stops s
         JOIN routes r ON r.uid=s.route_uid AND r.account_id=?
         WHERE r.user_id IN ($ph)
           AND (r.date_assigned=? OR r.scheduled_dates LIKE CONCAT('%',?,'%'))
           AND s.deleted_at IS NULL
         GROUP BY r.user_id"
    );
    $paramsS = array_merge([$aid], $agentIds, [$today, $today]);
    $stS->execute($paramsS);
    $stopsMap = [];
    foreach ($stS->fetchAll() as $s) { $stopsMap[$s['user_id']] = $s; }

    // Stops del mes para KPI mensual
    $monthStart = $month . '-01';
    $monthEnd   = date('Y-m-t', strtotime($monthStart));
    $stM = db()->prepare(
        "SELECT r.user_id,
                COUNT(s.id)                         AS total_month,
                SUM(s.status='done')                AS done_month,
                SUM(s.visit_result='contactado')    AS contacted_month
         FROM stops s
         JOIN routes r ON r.uid=s.route_uid AND r.account_id=?
         WHERE r.user_id IN ($ph)
           AND r.date_assigned BETWEEN ? AND ?
           AND s.deleted_at IS NULL
         GROUP BY r.user_id"
    );
    $paramsM = array_merge([$aid], $agentIds, [$monthStart, $monthEnd]);
    $stM->execute($paramsM);
    $monthMap = [];
    foreach ($stM->fetchAll() as $m) { $monthMap[$m['user_id']] = $m; }

    $result = [];
    foreach ($agents as $ag) {
        $agId    = (int)$ag['id'];
        $jornada = $jornadas[$agId] ?? null;
        $stops   = $stopsMap[$agId] ?? null;
        $month_s = $monthMap[$agId]  ?? null;
        $result[] = [
            'user_id'       => $agId,
            'name'          => $ag['name'],
            'username'      => $ag['username'],
            'role'          => $ag['role'],
            'avatar_url'    => $ag['avatar_url'],
            // Jornada hoy
            'jornada_state'   => $jornada['state']        ?? null,
            'elapsed_ms'      => $jornada['elapsed_ms']   ?? 0,
            'distance_km'     => $jornada['distance_km']  ?? 0,
            'last_lat'        => $jornada['last_lat']      ?? null,
            'last_lng'        => $jornada['last_lng']      ?? null,
            'last_gps_at'     => $jornada['updated_at']   ?? null,
            // Stops hoy
            'stops_total'     => (int)($stops['total']     ?? 0),
            'stops_done'      => (int)($stops['done']      ?? 0),
            'stops_skipped'   => (int)($stops['skipped']   ?? 0),
            'stops_contacted' => (int)($stops['contacted'] ?? 0),
            // KPIs mes
            'month_total'     => (int)($month_s['total_month']      ?? 0),
            'month_done'      => (int)($month_s['done_month']       ?? 0),
            'month_contacted' => (int)($month_s['contacted_month']  ?? 0),
        ];
    }
    ok(['agents' => $result]);
}

// ─── S20: agent_detail ─────────────────────────────────────────────────────
if ($action === 'agent_detail') {
    $sess     = requireAuth();
    $callerId = (int)$sess['uid'];
    $aid      = (int)$sess['account_id'];
    $callerRole = $sess['role'];

    if (roleLevel($callerRole) < 3) err('Sin permisos', 403);

    $targetId = (int)($_GET['user_id'] ?? 0);
    if (!$targetId) err('user_id requerido');

    // Verificar que el caller puede ver este agente
    if ($callerRole === 'manager') {
        $chk = db()->prepare('SELECT id FROM users WHERE id=? AND manager_id=? AND account_id=? LIMIT 1');
        $chk->execute([$targetId, $callerId, $aid]);
        if (!$chk->fetch()) err('No tienes acceso a este agente', 403);
    } elseif (roleLevel($callerRole) < 4) {
        err('Sin permisos para ver detalles de agente', 403);
    }

    $today      = date('Y-m-d');
    $month      = date('Y-m');
    $monthStart = $month . '-01';
    $monthEnd   = date('Y-m-t', strtotime($monthStart));

    // Info del agente
    $stU = db()->prepare('SELECT id, name, username, role, email, avatar_url, created_at FROM users WHERE id=? AND account_id=? LIMIT 1');
    $stU->execute([$targetId, $aid]);
    $agent = $stU->fetch();
    if (!$agent) err('Agente no encontrado', 404);

    // Jornada de hoy
    $stJ = db()->prepare('SELECT * FROM day_sessions WHERE user_id=? AND date_str=? LIMIT 1');
    $stJ->execute([$targetId, $today]);
    $jornada = $stJ->fetch() ?: null;

    // Rutas y stops de hoy
    $stR = db()->prepare(
        'SELECT r.uid, r.name, r.status, r.date_assigned,
                COUNT(s.id)               AS total_stops,
                SUM(s.status="done")      AS done_stops,
                SUM(s.status="skipped")   AS skipped_stops,
                SUM(s.status="pending")   AS pending_stops
         FROM routes r
         LEFT JOIN stops s ON s.route_uid=r.uid AND s.deleted_at IS NULL
         WHERE r.user_id=? AND r.account_id=?
           AND (r.date_assigned=? OR r.scheduled_dates LIKE CONCAT("%",?,"%"))
           AND r.deleted_at IS NULL
         GROUP BY r.uid, r.name, r.status, r.date_assigned'
    );
    $stR->execute([$targetId, $aid, $today, $today]);
    $todayRoutes = $stR->fetchAll();

    // Historial últimas 5 visitas completadas
    $stV = db()->prepare(
        'SELECT s.uid, s.name, s.visit_result, s.visited_at, s.next_action,
                s.gps_lat_visit, s.gps_lng_visit, r.name AS route_name
         FROM stops s JOIN routes r ON r.uid=s.route_uid
         WHERE r.user_id=? AND r.account_id=? AND s.status="done" AND s.deleted_at IS NULL
         ORDER BY s.visited_at DESC LIMIT 10'
    );
    $stV->execute([$targetId, $aid]);
    $recentVisits = $stV->fetchAll();

    // KPIs del mes
    $stKpi = db()->prepare(
        'SELECT COUNT(s.id) AS total, SUM(s.status="done") AS done,
                SUM(s.visit_result="contactado") AS contacted,
                SUM(s.visit_result="no_estaba") AS not_home,
                SUM(s.visit_result="rechazado") AS rejected
         FROM stops s JOIN routes r ON r.uid=s.route_uid AND r.account_id=?
         WHERE r.user_id=? AND r.date_assigned BETWEEN ? AND ? AND s.deleted_at IS NULL'
    );
    $stKpi->execute([$aid, $targetId, $monthStart, $monthEnd]);
    $kpis = $stKpi->fetch();

    ok([
        'agent'        => $agent,
        'jornada'      => $jornada,
        'today_routes' => $todayRoutes,
        'recent_visits'=> $recentVisits,
        'month_kpis'   => $kpis,
    ]);
}

err("Acción desconocida: {$action}", 404);




