-- ============================================================
-- Migration v6 — Jornadas + KPI values + columnas stops
-- Aplicar en: cqvkelal_rutasapp_android
-- ============================================================

-- ── 1. Columnas faltantes en stops ───────────────────────────
-- visit_result y next_action existían en app pero no en servidor
ALTER TABLE `stops`
    ADD COLUMN IF NOT EXISTS `external_id`    VARCHAR(100)  DEFAULT NULL AFTER `order_index`,
    ADD COLUMN IF NOT EXISTS `contact_name`   VARCHAR(255)  DEFAULT NULL AFTER `external_id`,
    ADD COLUMN IF NOT EXISTS `contact_phone`  VARCHAR(50)   DEFAULT NULL AFTER `contact_name`,
    ADD COLUMN IF NOT EXISTS `visit_result`   VARCHAR(20)   DEFAULT NULL AFTER `visited_at`,
    ADD COLUMN IF NOT EXISTS `next_action`    TEXT          DEFAULT NULL AFTER `visit_result`;

-- ── 2. Tabla day_sessions ──────────────────────────────────
-- Jornada laboral por ruta y día
CREATE TABLE IF NOT EXISTS `day_sessions` (
    `id`            INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    `account_id`    INT UNSIGNED     NOT NULL,
    `user_id`       INT UNSIGNED     NOT NULL,
    `route_uid`     VARCHAR(36)      NOT NULL,
    `date_str`      DATE             NOT NULL,
    `state`         ENUM('idle','running','paused','done') NOT NULL DEFAULT 'idle',
    `started_at`    BIGINT           DEFAULT NULL,   -- epoch ms
    `paused_at`     BIGINT           DEFAULT NULL,
    `elapsed_ms`    BIGINT           NOT NULL DEFAULT 0,
    `distance_km`   DECIMAL(8,3)     NOT NULL DEFAULT 0.000,
    `last_lat`      DECIMAL(10,7)    DEFAULT NULL,
    `last_lng`      DECIMAL(10,7)    DEFAULT NULL,
    `updated_at`    BIGINT           NOT NULL DEFAULT 0,
    `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `synced_at`     DATETIME         DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_session` (`user_id`, `route_uid`, `date_str`),
    KEY `idx_ds_user_date` (`user_id`, `date_str`),
    KEY `idx_ds_account`   (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 3. Tabla kpi_values ───────────────────────────────────────
-- Un valor por KPI por visita (stop)
CREATE TABLE IF NOT EXISTS `kpi_values` (
    `id`            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `account_id`    INT UNSIGNED     NOT NULL,
    `stop_uid`      VARCHAR(36)      NOT NULL,
    `kpi_id`        VARCHAR(100)     NOT NULL,   -- e.g. "telco_activaciones"
    `value_text`    TEXT             DEFAULT NULL,
    `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_kpi_value` (`stop_uid`, `kpi_id`),
    KEY `idx_kv_account`  (`account_id`),
    KEY `idx_kv_stop`     (`stop_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 4. Tabla business_profiles ───────────────────────────────
-- Sector y config de KPIs por cuenta
CREATE TABLE IF NOT EXISTS `business_profiles` (
    `account_id`    INT UNSIGNED     NOT NULL,
    `sector`        VARCHAR(50)      NOT NULL DEFAULT 'custom',
    `name`          VARCHAR(255)     NOT NULL DEFAULT 'Mi negocio',
    `updated_at`    BIGINT           NOT NULL DEFAULT 0,
    PRIMARY KEY (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 5. Registrar migración ────────────────────────────────────
INSERT IGNORE INTO `schema_migrations` (`version`, `applied_at`)
VALUES ('6.0.0', NOW());
