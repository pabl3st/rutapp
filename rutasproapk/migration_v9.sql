-- migration_v9.sql — sync de vacaciones del calendario
-- Aplicar en phpMyAdmin sobre cqvkelal_rutasapp_android

-- Las vacaciones se almacenan como JSON dentro de user_prefs.prefs
-- bajo la clave "vacation_days" (array de fechas ISO: ["2026-05-12","2026-06-01"])
-- No requiere nuevas columnas — se añade como campo dentro del JSON existente.
-- update_user_prefs ya acepta y almacena vacation_days desde api.php v9.

-- Verificar que user_prefs tiene la estructura correcta:
-- CREATE TABLE IF NOT EXISTS `user_prefs` (
--   `user_id` int NOT NULL,
--   `prefs` JSON DEFAULT NULL,
--   `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--   PRIMARY KEY (`user_id`)
-- );

INSERT IGNORE INTO `schema_migrations` (`version`) VALUES ('v9.0.0');
