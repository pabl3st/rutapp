-- ============================================================
-- Migration v6.0.0 — KPI values + pdv_open en stops
-- Aplicar DESPUÉS de migration_v5.sql
-- ============================================================

SET NAMES utf8mb4;

-- ── kpi_values ───────────────────────────────────────────────
-- Un valor por KPI por stop. Sincronización bidireccional.
CREATE TABLE IF NOT EXISTS `kpi_values` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `account_id`  INT UNSIGNED    NOT NULL,
    `stop_uid`    VARCHAR(36)     NOT NULL,
    `kpi_id`      VARCHAR(100)    NOT NULL,
    `value_text`  TEXT            NOT NULL,
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `synced_at`   DATETIME        DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_stop_kpi`    (`stop_uid`, `kpi_id`),
    KEY `idx_account`           (`account_id`),
    KEY `idx_stop`              (`stop_uid`),
    KEY `idx_updated`           (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── pdv_open en stops ────────────────────────────────────────
-- Indica si el PDV estaba abierto en el momento de la última visita.
ALTER TABLE `stops`
    ADD COLUMN IF NOT EXISTS `pdv_open` TINYINT(1) NOT NULL DEFAULT 1 AFTER `opening_hours`;

INSERT INTO schema_migrations (version, applied_at) VALUES ('6.0.0', NOW())
    ON DUPLICATE KEY UPDATE applied_at = applied_at;
