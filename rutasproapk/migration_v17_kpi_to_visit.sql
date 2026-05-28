-- migration_v17_kpi_to_visit.sql
-- Los KPIs ya no se anclan al stop sino a la visita concreta.
-- Esto permite que un PDV con 4 visitas mensuales tenga 4 sets de KPIs.
--
-- Compatibilidad atrás: se conservan stop_uid y el índice no-único idx_kv_stop
-- para queries históricas por PDV.

-- Añadir columna visit_uid (MariaDB 10.6 soporta IF NOT EXISTS en ADD COLUMN)
ALTER TABLE kpi_values
  ADD COLUMN IF NOT EXISTS `visit_uid` varchar(36) DEFAULT NULL AFTER `stop_uid`;

-- Añadir índice (MariaDB no soporta IF NOT EXISTS en ADD KEY, usamos CREATE INDEX condicional)
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'kpi_values' AND index_name = 'idx_visit_uid'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_visit_uid ON kpi_values (visit_uid)',
  'SELECT "idx_visit_uid ya existe" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Back-fill: cada KPI existente apunta a la visita inicial del stop (creada en v16)
UPDATE kpi_values kv
JOIN stop_visits sv ON sv.uid = CONCAT(kv.stop_uid, '-v1')
SET kv.visit_uid = sv.uid
WHERE kv.visit_uid IS NULL;

-- Cambio de UNIQUE KEY: quitar (stop_uid, kpi_id) y poner (visit_uid, kpi_id).
-- La key antigua se convierte en índice no-único (idx_kv_stop ya existe).
SET @uq_old := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'kpi_values' AND index_name = 'uq_kpi_value'
);
SET @sql := IF(@uq_old > 0,
  'ALTER TABLE kpi_values DROP INDEX uq_kpi_value',
  'SELECT "uq_kpi_value ya no existe" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @uq_new := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'kpi_values' AND index_name = 'uq_visit_kpi'
);
SET @sql := IF(@uq_new = 0,
  'ALTER TABLE kpi_values ADD UNIQUE KEY uq_visit_kpi (visit_uid, kpi_id)',
  'SELECT "uq_visit_kpi ya existe" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Registrar la migración
INSERT INTO schema_migrations (version, applied_at)
VALUES ('v17_kpi_to_visit', NOW())
ON DUPLICATE KEY UPDATE applied_at = applied_at;
