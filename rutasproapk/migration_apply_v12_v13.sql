-- ══════════════════════════════════════════════════════════════
-- MIGRACIONES v12 + v13 — SQL puro sin procedimientos ni INFORMATION_SCHEMA
-- Compatible con cPanel / hosting compartido
-- 
-- INSTRUCCIONES:
-- 1. Ir a cPanel → phpMyAdmin → seleccionar tu base de datos
-- 2. Pestaña "SQL" → pegar este contenido → Ejecutar
-- 3. Si aparece error "Duplicate column name" en algún ALTER TABLE,
--    significa que esa columna ya existe — ignorar el error y continuar
--    ejecutando el resto de sentencias una a una.
-- ══════════════════════════════════════════════════════════════

-- Tabla de control (segura — CREATE IF NOT EXISTS nunca falla)
CREATE TABLE IF NOT EXISTS schema_migrations (
    version     INT          NOT NULL PRIMARY KEY,
    applied_at  DATETIME     NOT NULL DEFAULT NOW(),
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── v12 ── Ejecutar cada ALTER por separado si hay errores ───
ALTER TABLE stops ADD COLUMN check_in_ts   BIGINT NULL COMMENT 'Epoch ms apertura formulario';
ALTER TABLE stops ADD COLUMN check_out_ts  BIGINT NULL COMMENT 'Epoch ms guardado visita';
ALTER TABLE stops ADD COLUMN gps_lat_visit DOUBLE NULL COMMENT 'Lat GPS agente al check-in';
ALTER TABLE stops ADD COLUMN gps_lng_visit DOUBLE NULL COMMENT 'Lng GPS agente al check-in';
INSERT IGNORE INTO schema_migrations (version, description) VALUES (12, 'check_in/out + gps_visit en stops');

-- ── v13 ── date_assigned en stops ────────────────────────────
ALTER TABLE stops ADD COLUMN date_assigned DATE NULL COMMENT 'Fecha visita concreta del stop';
INSERT IGNORE INTO schema_migrations (version, description) VALUES (13, 'date_assigned en stops');

-- ── manager_id en users ───────────────────────────────────────
ALTER TABLE users ADD COLUMN manager_id INT NULL COMMENT 'FK supervisor directo';
ALTER TABLE users ADD INDEX idx_users_manager (manager_id);
INSERT IGNORE INTO schema_migrations (version, description) VALUES (14, 'manager_id en users');

-- ── Verificar ─────────────────────────────────────────────────
SELECT version, applied_at, description FROM schema_migrations ORDER BY version;
