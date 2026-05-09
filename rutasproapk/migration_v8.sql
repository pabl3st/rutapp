-- migration_v8.sql — scheduledDates en routes
-- Aplicar en phpMyAdmin sobre cqvkelal_rutasapp_android

ALTER TABLE `routes`
    ADD COLUMN IF NOT EXISTS `scheduled_dates` TEXT DEFAULT NULL
    COMMENT 'JSON array de fechas de visita: ["2026-05-12","2026-05-21"]';

INSERT IGNORE INTO `schema_migrations` (`version`) VALUES ('v8.0.0');
