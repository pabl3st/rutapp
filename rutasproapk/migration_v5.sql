-- ============================================================
-- Migration v5.0.0 — stops: columnas faltantes para sync completo
-- Aplicar en: cqvkelal_rutasapp_android
-- ============================================================

-- Campos de identificación de cliente
ALTER TABLE `stops`
    ADD COLUMN `external_id`   varchar(100)  DEFAULT NULL AFTER `account_id`,
    ADD COLUMN `contact_name`  varchar(255)  DEFAULT NULL AFTER `opening_hours`,
    ADD COLUMN `contact_phone` varchar(50)   DEFAULT NULL AFTER `contact_name`;

-- Campos de resultado de visita (VisitaScreen)
ALTER TABLE `stops`
    ADD COLUMN `visit_result`  varchar(20)   DEFAULT NULL AFTER `visited_at`,
    ADD COLUMN `next_action`   text          DEFAULT NULL AFTER `visit_result`;

-- Índice para búsqueda por external_id (código de cliente)
ALTER TABLE `stops`
    ADD KEY `idx_external_id` (`external_id`);

-- Registrar migración
INSERT INTO `schema_migrations` (`version`) VALUES ('v5.0.0');
