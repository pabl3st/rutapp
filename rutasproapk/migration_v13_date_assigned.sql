-- Migration v13: date_assigned en stops (informes diarios independientes)
-- Aplicar SOLO si check_in_ts ya existe (migration_v12 ya aplicada)
-- Verificar antes: SHOW COLUMNS FROM stops LIKE 'date_assigned';
-- Si no aparece ninguna fila, ejecutar:

ALTER TABLE stops
  ADD COLUMN date_assigned DATE NULL
  COMMENT 'Fecha de esta visita concreta — 1 stop por fecha por PDV (informes independientes)'
  AFTER gps_lng_visit;

-- Índice para mejorar el rendimiento del filtro por fecha en RouteDetailScreen
CREATE INDEX idx_stops_route_date
  ON stops (route_id, date_assigned);
