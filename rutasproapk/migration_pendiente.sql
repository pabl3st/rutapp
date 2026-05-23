-- ══════════════════════════════════════════════════════════════
-- ÚNICA MIGRACIÓN PENDIENTE — solo date_assigned
-- Los campos check_in_ts/out, gps_lat/lng_visit YA están en prod
-- 
-- Ejecutar en phpMyAdmin → pestaña SQL
-- Si da error 1060 "Duplicate column name" → ya estaba, ignorar
-- ══════════════════════════════════════════════════════════════

ALTER TABLE stops 
  ADD COLUMN date_assigned DATE NULL 
  COMMENT 'Fecha de esta visita concreta (informe independiente por dia)';

CREATE INDEX idx_stops_route_date 
  ON stops (route_id, date_assigned);
