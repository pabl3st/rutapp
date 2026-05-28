-- migration_v16_stop_visits.sql
-- Modelo de informes diarios independientes:
-- Una parada (stop) puede visitarse N veces (una por fecha programada).
-- Cada visita es una fila en stop_visits — append-only en sentido lógico,
-- sync bidireccional como cualquier entidad.

CREATE TABLE IF NOT EXISTS `stop_visits` (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `uid` varchar(36) NOT NULL,
  `stop_uid` varchar(36) NOT NULL,
  `route_uid` varchar(36) NOT NULL,
  `account_id` int(10) UNSIGNED NOT NULL,
  `visit_date` date NOT NULL,
  `status` enum('pending','visiting','done','skipped') NOT NULL DEFAULT 'pending',
  `visited_at` datetime DEFAULT NULL,
  `visit_result` varchar(20) DEFAULT NULL,
  `next_action` text DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `check_in_ts` bigint(20) DEFAULT NULL,
  `check_out_ts` bigint(20) DEFAULT NULL,
  `gps_lat_visit` double DEFAULT NULL,
  `gps_lng_visit` double DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_visit_uid` (`uid`),
  UNIQUE KEY `uq_stop_date` (`stop_uid`, `visit_date`),
  KEY `idx_route_date` (`route_uid`, `visit_date`),
  KEY `idx_account_updated` (`account_id`, `updated_at`),
  CONSTRAINT `fk_visit_stop` FOREIGN KEY (`stop_uid`) REFERENCES `stops` (`uid`) ON DELETE CASCADE,
  CONSTRAINT `fk_visit_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Back-fill: para cada stop existente con visitedAt o status != 'pending',
-- crear una visita inicial con sus datos actuales.
INSERT INTO stop_visits (uid, stop_uid, route_uid, account_id, visit_date,
                         status, visited_at, visit_result, next_action, notes,
                         check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
                         created_at, updated_at)
SELECT
    CONCAT(s.uid, '-v1') AS uid,
    s.uid AS stop_uid,
    r.uid AS route_uid,
    s.account_id,
    COALESCE(s.date_assigned, DATE(s.created_at)) AS visit_date,
    s.status,
    s.visited_at,
    s.visit_result,
    s.next_action,
    s.notes,
    s.check_in_ts,
    s.check_out_ts,
    s.gps_lat_visit,
    s.gps_lng_visit,
    s.created_at,
    s.updated_at
FROM stops s
JOIN routes r ON r.id = s.route_id
WHERE s.deleted_at IS NULL
  AND (s.visited_at IS NOT NULL OR s.status != 'pending' OR s.date_assigned IS NOT NULL)
ON DUPLICATE KEY UPDATE updated_at = stop_visits.updated_at;

-- Registrar la migración
INSERT INTO schema_migrations (version, applied_at)
VALUES ('v16_stop_visits', NOW())
ON DUPLICATE KEY UPDATE applied_at = applied_at;
