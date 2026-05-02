-- ============================================================
--  RutasApp Android · Migration v1.0.0
--  Base de datos: cqvkelal_rutasapp_android
--  Ejecutar UNA sola vez. Todas las instrucciones son idempotentes.
--  Charset: utf8mb4 · Engine: InnoDB
-- ============================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET foreign_key_checks = 0;

-- ── accounts ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS accounts (
    id           INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    type         ENUM('individual','company') NOT NULL DEFAULT 'individual',
    name         VARCHAR(255)    NOT NULL,
    slug         VARCHAR(100)    NOT NULL,
    plan         ENUM('free','pro','business') NOT NULL DEFAULT 'free',
    plus_config  JSON            NULL,
    form_config  JSON            NULL,
    ai_settings  JSON            NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── users ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    account_id    INT UNSIGNED    NOT NULL,
    username      VARCHAR(100)    NOT NULL,
    email         VARCHAR(255)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    name          VARCHAR(255)    NOT NULL,
    role          ENUM('owner','admin','manager','agent','viewer') NOT NULL DEFAULT 'owner',
    active        TINYINT(1)      NOT NULL DEFAULT 1,
    avatar_url    VARCHAR(500)    NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email (email),
    KEY idx_account (account_id),
    CONSTRAINT fk_users_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── sessions ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sessions (
    id           INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    user_id      INT UNSIGNED    NOT NULL,
    token        CHAR(64)        NOT NULL,
    device_id    VARCHAR(255)    NOT NULL,
    device_name  VARCHAR(255)    NULL,
    platform     ENUM('android','ios','web') NOT NULL DEFAULT 'android',
    app_version  VARCHAR(30)     NULL,
    fcm_token    TEXT            NULL,
    expires_at   DATETIME        NOT NULL,
    last_used_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_token (token),
    UNIQUE KEY uq_user_device (user_id, device_id),
    KEY idx_user (user_id),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── invite_codes ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS invite_codes (
    id             INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    account_id     INT UNSIGNED    NOT NULL,
    created_by     INT UNSIGNED    NOT NULL,
    code           VARCHAR(20)     NOT NULL,
    role_to_assign VARCHAR(20)     NOT NULL DEFAULT 'agent',
    uses_left      TINYINT UNSIGNED NOT NULL DEFAULT 1,
    expires_at     DATETIME        NOT NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_code (code),
    KEY idx_account (account_id),
    CONSTRAINT fk_invite_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_invite_user    FOREIGN KEY (created_by)  REFERENCES users(id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── user_prefs ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_prefs (
    user_id    INT UNSIGNED NOT NULL,
    prefs      JSON         NOT NULL,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_prefs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── login_attempts ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS login_attempts (
    id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
    ip_address     VARCHAR(45)  NOT NULL,
    username_tried VARCHAR(100) NULL,
    success        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ip_time (ip_address, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── rate_limits ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rate_limits (
    id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `key` VARCHAR(120)  NOT NULL,
    ts  INT UNSIGNED    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_key_ts (`key`, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── api_logs ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS api_logs (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    action      VARCHAR(60)     NOT NULL,
    user_id     INT UNSIGNED    NOT NULL DEFAULT 0,
    account_id  INT UNSIGNED    NOT NULL DEFAULT 0,
    ip          VARCHAR(45)     NOT NULL,
    method      VARCHAR(10)     NOT NULL,
    status      SMALLINT        NOT NULL DEFAULT 200,
    error_msg   VARCHAR(500)    NULL,
    duration_ms SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_action (action),
    KEY idx_user (user_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── schema_migrations ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS schema_migrations (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    version    VARCHAR(30)  NOT NULL,
    applied_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Stops, routes, etc. — se añaden en S02+ ─────────────────

INSERT IGNORE INTO schema_migrations (version) VALUES ('v1.0.0');

SET foreign_key_checks = 1;

-- ============================================================
-- FIN migration_v1.sql
-- Ejecutar en phpMyAdmin seleccionando la BD cqvkelal_rutasapp_android
-- ============================================================
