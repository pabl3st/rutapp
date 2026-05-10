-- ============================================================
-- seed_new_company_user.sql
-- Crea un account de tipo 'company' + usuario owner
-- con la MISMA contraseña que el usuario existente (user_id=2)
--
-- Ejecutar en phpMyAdmin sobre cqvkelal_rutasapp_android
-- ============================================================

SET NAMES utf8mb4;

-- ── 1. Crear account de tipo company ─────────────────────────
INSERT INTO `accounts`
    (type, name, slug, plan)
VALUES
    ('company', 'Empresa Demo SA', 'empresa-demo-sa', 'free');

-- Guardar el id del account recién creado
SET @new_account_id = LAST_INSERT_ID();

-- ── 2. Crear usuario owner con mismo hash que user_id=2 ──────
INSERT INTO `users`
    (account_id, username, email, password_hash, name, role, active)
SELECT
    @new_account_id,
    'empresa_owner',
    'empresa@rutasapp.dev',
    password_hash,   -- mismo hash → misma contraseña
    'Owner Empresa',
    'owner',
    1
FROM `users`
WHERE id = 2        -- usuario de referencia (cambia si el tuyo tiene otro id)
LIMIT 1;

SET @new_user_id = LAST_INSERT_ID();

-- ── 3. Prefs vacías ───────────────────────────────────────────
INSERT INTO `user_prefs` (user_id, prefs)
VALUES (@new_user_id, '{}');

-- ── 4. Verificación ──────────────────────────────────────────
SELECT
    u.id        AS user_id,
    u.username,
    u.email,
    u.role,
    u.active,
    a.id        AS account_id,
    a.type      AS account_type,
    a.name      AS account_name,
    a.slug
FROM `users` u
JOIN `accounts` a ON a.id = u.account_id
WHERE u.id = @new_user_id;
