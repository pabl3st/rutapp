-- RutasApp Android · Migration v3.0.0
-- S04: external_id en stops + campos contacto y resultado visita (preparados para S06)
-- Ejecutar en phpMyAdmin sobre cqvkelal_rutasapp_android

ALTER TABLE `stops`
    ADD COLUMN `external_id`    VARCHAR(100) DEFAULT NULL    AFTER `name`,
    ADD COLUMN `contact_name`   VARCHAR(255) DEFAULT NULL    AFTER `notes`,
    ADD COLUMN `contact_phone`  VARCHAR(50)  DEFAULT NULL    AFTER `contact_name`,
    ADD COLUMN `visit_result`   VARCHAR(50)  DEFAULT NULL    AFTER `visited_at`,
    ADD COLUMN `next_action`    TEXT         DEFAULT NULL    AFTER `visit_result`;

-- Índice para búsqueda por external_id (import/export bulk)
ALTER TABLE `stops`
    ADD INDEX `idx_external_id` (`external_id`),
    ADD INDEX `idx_account_external` (`account_id`, `external_id`);

INSERT IGNORE INTO `schema_migrations` (`version`) VALUES ('v3.0.0');
