-- migration_v18_photos_to_visit.sql
-- Las fotos pasan a estar ancladas a la visita concreta, no al stop.
-- Compatibilidad atrás: se conserva stop_uid (FK lógica a stops.uid) e idx_vp_stop.

ALTER TABLE visit_photos
  ADD COLUMN IF NOT EXISTS `visit_uid` varchar(36) DEFAULT NULL AFTER `stop_uid`;

-- Añadir índice idx_visit_uid si no existe (MariaDB no soporta IF NOT EXISTS en ADD KEY)
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'visit_photos' AND index_name = 'idx_visit_uid'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_visit_uid ON visit_photos (visit_uid)',
  'SELECT "idx_visit_uid ya existe" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Back-fill: cada foto apunta a la visita inicial del stop (creada en v16)
-- Importante: visit_photos usa utf8mb4_general_ci pero stop_visits usa
-- utf8mb4_unicode_ci, así que forzamos collation en la comparación del JOIN
-- (MariaDB error #1267 si no).
UPDATE visit_photos vp
JOIN stop_visits sv
  ON sv.uid COLLATE utf8mb4_unicode_ci = CONCAT(vp.stop_uid, '-v1') COLLATE utf8mb4_unicode_ci
SET vp.visit_uid = sv.uid
WHERE vp.visit_uid IS NULL;

INSERT INTO schema_migrations (version, applied_at)
VALUES ('v18_photos_to_visit', NOW())
ON DUPLICATE KEY UPDATE applied_at = applied_at;
