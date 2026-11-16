-- ============================================================================
-- RutasApp v19 -- GPS de entrada, ubicacion en vivo y reparacion de estado
-- Ejecutar UNA VEZ en phpMyAdmin (base cqvkelal_rutasapp_android).
-- Es idempotente: se puede repetir sin romper nada.
-- ============================================================================

-- 1) GPS de ENTRADA de la visita (el de salida ya existe: gps_lat_visit/gps_lng_visit)
ALTER TABLE stop_visits
  ADD COLUMN IF NOT EXISTS gps_lat_in DOUBLE DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS gps_lng_in DOUBLE DEFAULT NULL;

-- 2) Ultima ubicacion en vivo de cada gestor (sin FK a proposito: ERROR 20 de cPanel)
CREATE TABLE IF NOT EXISTS agent_locations (
  user_id    INT UNSIGNED NOT NULL,
  account_id INT UNSIGNED NOT NULL,
  lat        DOUBLE NOT NULL,
  lng        DOUBLE NOT NULL,
  accuracy   DOUBLE DEFAULT NULL,
  route_uid  VARCHAR(36) DEFAULT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  KEY idx_agent_loc_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) REPARACION: rutas marcadas 'done' que tienen visitas pendientes o en curso
--    en hoy o fechas futuras (p.ej. PS03: completada en agosto y reasignada en
--    noviembre). El estado real de cada dia vive en stop_visits.
UPDATE routes r
   SET r.status = 'pending', r.updated_at = NOW()
 WHERE r.status = 'done'
   AND r.deleted_at IS NULL
   AND EXISTS (
         SELECT 1 FROM stop_visits sv
          WHERE sv.route_uid = r.uid
            AND sv.deleted_at IS NULL
            AND sv.status IN ('pending','visiting')
            AND sv.visit_date >= CURDATE()
       );
