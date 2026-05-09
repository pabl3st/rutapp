-- ============================================================
-- Migration v5.0.0 — Jornadas, KPI definitions, Business profiles
-- Aplicar DESPUÉS de migration_v4.sql
-- ============================================================

SET NAMES utf8mb4;

-- ── day_sessions ──────────────────────────────────────────────
-- Jornada de trabajo por ruta y día. Una fila por (user, route, date).
CREATE TABLE IF NOT EXISTS `day_sessions` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `account_id`   INT UNSIGNED    NOT NULL,
    `user_id`      INT UNSIGNED    NOT NULL,
    `route_uid`    VARCHAR(36)     NOT NULL,
    `date_str`     VARCHAR(10)     NOT NULL,  -- YYYY-MM-DD
    `state`        VARCHAR(10)     NOT NULL DEFAULT 'idle',  -- idle|running|paused|done
    `started_at`   BIGINT          DEFAULT NULL,  -- epoch ms
    `elapsed_ms`   BIGINT          NOT NULL DEFAULT 0,
    `distance_km`  DECIMAL(8,3)    NOT NULL DEFAULT 0.000,
    `last_lat`     DECIMAL(10,7)   DEFAULT NULL,
    `last_lng`     DECIMAL(10,7)   DEFAULT NULL,
    `updated_at`   BIGINT          NOT NULL DEFAULT 0,  -- epoch ms (consistente con cliente)
    `synced_at`    DATETIME        DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_session` (`user_id`, `route_uid`, `date_str`),
    KEY `idx_account_date` (`account_id`, `date_str`),
    KEY `idx_user_date`    (`user_id`, `date_str`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── kpi_definitions ──────────────────────────────────────────
-- Definiciones de KPIs por sector. account_id=0 = sistema.
CREATE TABLE IF NOT EXISTS `kpi_definitions` (
    `id`          VARCHAR(100)    NOT NULL,
    `account_id`  INT UNSIGNED    NOT NULL DEFAULT 0,
    `sector`      VARCHAR(50)     NOT NULL,
    `label`       VARCHAR(255)    NOT NULL,
    `type`        VARCHAR(20)     NOT NULL,  -- number|boolean|select|text
    `unit`        VARCHAR(20)     DEFAULT NULL,
    `options`     TEXT            DEFAULT NULL,  -- JSON array para type=select
    `required`    TINYINT(1)      NOT NULL DEFAULT 0,
    `visible`     TINYINT(1)      NOT NULL DEFAULT 1,
    `order_index` INT             NOT NULL DEFAULT 0,
    `section`     VARCHAR(50)     NOT NULL DEFAULT 'general',
    `is_system`   TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sector`  (`sector`),
    KEY `idx_account` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── business_profiles ────────────────────────────────────────
-- Perfil de negocio por account. Un registro por account.
CREATE TABLE IF NOT EXISTS `business_profiles` (
    `account_id`  INT UNSIGNED  NOT NULL,
    `sector`      VARCHAR(50)   NOT NULL DEFAULT 'custom',
    `name`        VARCHAR(255)  NOT NULL DEFAULT 'Mi negocio',
    `updated_at`  BIGINT        NOT NULL DEFAULT 0,
    `synced_at`   DATETIME      DEFAULT NULL,
    PRIMARY KEY (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO schema_migrations (version, applied_at) VALUES ('5.0.0', NOW())
    ON DUPLICATE KEY UPDATE applied_at = applied_at;
