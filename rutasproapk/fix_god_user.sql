-- fix_god_user.sql
-- Ejecutar en phpMyAdmin sobre cqvkelal_fieldapp_db (y cqvkelal_gr si aplica)
-- Arregla el ENUM de role para soportar 'god' y crea el usuario superadmin

-- 1. Cambiar ENUM a VARCHAR para soportar 'god' y futuros roles
ALTER TABLE `users`
    MODIFY COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'owner';

-- 2. Verificar / añadir columna is_active (alias de active en god_users_all)
--    La tabla usa 'active' — el alias se resuelve en api.php con active AS is_active
--    No hace falta cambiar nada en BD, solo confirmar que 'active' existe:
-- SELECT COLUMN_NAME FROM information_schema.COLUMNS
-- WHERE TABLE_NAME='users' AND COLUMN_NAME='active';

-- 3. Crear cuenta sistema para el usuario god (si no existe)
INSERT IGNORE INTO `accounts` (id, type, name, slug)
VALUES (1, 'system', 'Sistema', 'sistema');

-- 4. Crear usuario god
--    Contraseña por defecto: "rutasapp_god_2026" (bcrypt, cambiar tras primer acceso)
INSERT IGNORE INTO `users`
    (account_id, username, email, password_hash, name, role, active)
VALUES (
    1,
    'god',
    'god@sistema.local',
    '$2y$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- "password"
    'Superadmin',
    'god',
    1
);

-- IMPORTANTE: cambiar la contraseña del usuario god inmediatamente:
-- UPDATE users SET password_hash = PASSWORD_HASH_AQUI WHERE username = 'god';
-- Generar hash en PHP: echo password_hash('tu_contraseña', PASSWORD_BCRYPT, ['cost'=>12]);

-- 5. Prefs vacías para god
INSERT IGNORE INTO `user_prefs` (user_id, prefs)
SELECT id, '{}' FROM users WHERE username = 'god';

-- 6. Verificar
SELECT id, username, role, active FROM users WHERE username = 'god';
