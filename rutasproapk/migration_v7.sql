-- migration_v7.sql — Bloque 2: PDV inactivo permanente
-- Aplicar en phpMyAdmin sobre cqvkelal_fieldapp_db y cqvkelal_gr

-- 1. Añadir columna pdv_inactive a stops
ALTER TABLE `stops`
    ADD COLUMN IF NOT EXISTS `pdv_inactive` TINYINT(1) NOT NULL DEFAULT 0
    AFTER `pdv_open`;

-- 2. Índice para filtrar PDVs inactivos rápidamente
ALTER TABLE `stops`
    ADD KEY IF NOT EXISTS `idx_pdv_inactive` (`pdv_inactive`, `account_id`);

-- 3. Registrar migración
INSERT IGNORE INTO `schema_migrations` (`version`) VALUES ('v7.0.0');
