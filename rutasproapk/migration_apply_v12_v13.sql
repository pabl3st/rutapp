-- ══════════════════════════════════════════════════════════════
-- APLICAR MIGRACIONES v12 y v13 EN PRODUCCIÓN
-- Ejecutar en MySQL como: mysql -u user -p db_name < migration_apply_v12_v13.sql
-- Es idempotente — no falla si las columnas ya existen
-- ══════════════════════════════════════════════════════════════

-- Crear tabla de control de migraciones si no existe
CREATE TABLE IF NOT EXISTS schema_migrations (
    version     INT         NOT NULL PRIMARY KEY,
    applied_at  DATETIME    NOT NULL DEFAULT NOW(),
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── v12: check-in/out timestamps y GPS de visita ─────────────
SET @v12 = (SELECT COUNT(*) FROM schema_migrations WHERE version = 12);
SET @sql_v12a = IF(@v12 = 0 AND NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='stops' AND COLUMN_NAME='check_in_ts'
    AND TABLE_SCHEMA=DATABASE()
), 'ALTER TABLE stops ADD COLUMN check_in_ts BIGINT NULL COMMENT ''Epoch ms — cuando el agente abrió el formulario''', 'SELECT 1');
PREPARE stmt FROM @sql_v12a; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql_v12b = IF(@v12 = 0 AND NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='stops' AND COLUMN_NAME='check_out_ts'
    AND TABLE_SCHEMA=DATABASE()
), 'ALTER TABLE stops ADD COLUMN check_out_ts BIGINT NULL COMMENT ''Epoch ms — cuando se guardó la visita''', 'SELECT 1');
PREPARE stmt FROM @sql_v12b; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql_v12c = IF(@v12 = 0 AND NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='stops' AND COLUMN_NAME='gps_lat_visit'
    AND TABLE_SCHEMA=DATABASE()
), 'ALTER TABLE stops ADD COLUMN gps_lat_visit DOUBLE NULL COMMENT ''Latitud GPS del agente al check-in''', 'SELECT 1');
PREPARE stmt FROM @sql_v12c; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql_v12d = IF(@v12 = 0 AND NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='stops' AND COLUMN_NAME='gps_lng_visit'
    AND TABLE_SCHEMA=DATABASE()
), 'ALTER TABLE stops ADD COLUMN gps_lng_visit DOUBLE NULL COMMENT ''Longitud GPS del agente al check-in''', 'SELECT 1');
PREPARE stmt FROM @sql_v12d; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO schema_migrations (version, description)
VALUES (12, 'check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit en stops');

-- ── v13: date_assigned en stops (informes diarios independientes) ─
SET @v13 = (SELECT COUNT(*) FROM schema_migrations WHERE version = 13);
SET @sql_v13 = IF(@v13 = 0 AND NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='stops' AND COLUMN_NAME='date_assigned'
    AND TABLE_SCHEMA=DATABASE()
), 'ALTER TABLE stops ADD COLUMN date_assigned DATE NULL COMMENT ''Fecha de esta visita concreta'' AFTER gps_lng_visit', 'SELECT 1');
PREPARE stmt FROM @sql_v13; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO schema_migrations (version, description)
VALUES (13, 'date_assigned en stops');

-- ── manager_id en users (si no existe) ───────────────────────
SET @sql_mgr = IF(NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='users' AND COLUMN_NAME='manager_id'
    AND TABLE_SCHEMA=DATABASE()
), 'ALTER TABLE users ADD COLUMN manager_id INT NULL REFERENCES users(id)', 'SELECT 1');
PREPARE stmt FROM @sql_mgr; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── Índice en manager_id para queries de reportadores ────────
SET @sql_idx = IF(NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_NAME='users' AND INDEX_NAME='idx_users_manager'
    AND TABLE_SCHEMA=DATABASE()
), 'ALTER TABLE users ADD INDEX idx_users_manager (manager_id)', 'SELECT 1');
PREPARE stmt FROM @sql_idx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── Verificar resultado ───────────────────────────────────────
SELECT version, applied_at, description FROM schema_migrations ORDER BY version;
SELECT
    COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME='stops'
    AND COLUMN_NAME IN ('check_in_ts','check_out_ts','gps_lat_visit','gps_lng_visit','date_assigned')
    AND TABLE_SCHEMA=DATABASE()
ORDER BY ORDINAL_POSITION;
