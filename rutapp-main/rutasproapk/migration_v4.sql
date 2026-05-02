-- RutasApp Android · Migration v4.0.0
-- S05: campos universales stops (frecuencia visita, prioridad, segmento, estado cuenta, horarios)
-- Ejecutar en phpMyAdmin sobre cqvkelal_rutasapp_android

ALTER TABLE `stops`
    ADD COLUMN `visit_frequency`  SMALLINT UNSIGNED DEFAULT NULL        AFTER `order_index`,
    ADD COLUMN `priority`         TINYINT UNSIGNED  NOT NULL DEFAULT 3  AFTER `visit_frequency`,
    ADD COLUMN `segment`          VARCHAR(10)       DEFAULT NULL        AFTER `priority`,
    ADD COLUMN `account_status`   VARCHAR(20)       DEFAULT 'active'    AFTER `segment`,
    ADD COLUMN `opening_hours`    TEXT              DEFAULT NULL        AFTER `account_status`;

INSERT IGNORE INTO `schema_migrations` (`version`) VALUES ('v4.0.0');
