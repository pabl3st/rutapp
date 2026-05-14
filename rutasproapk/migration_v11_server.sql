-- migration_v11_server.sql — jerarquía de supervisores
-- Aplicar en phpMyAdmin sobre cqvkelal_rutasapp_android
-- NOTA: este archivo es solo para el servidor (no afecta Room Android)

SET FOREIGN_KEY_CHECKS=0;

-- Añadir manager_id a users — referencia al supervisor directo (nullable)
ALTER TABLE `users`
    ADD COLUMN IF NOT EXISTS `manager_id` INT UNSIGNED NULL DEFAULT NULL
        COMMENT 'supervisor directo — NULL = sin asignar'
        AFTER `role`;

-- Índice para lookups rápidos "dame los agentes de este manager"
ALTER TABLE `users`
    ADD KEY IF NOT EXISTS `idx_manager` (`manager_id`);

INSERT IGNORE INTO `schema_migrations` (`version`) VALUES ('v11_server');

SET FOREIGN_KEY_CHECKS=1;
