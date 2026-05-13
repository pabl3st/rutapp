-- RutasApp Android · Migration v2.0.0
-- Ejecutar en phpMyAdmin sobre cqvkelal_rutasapp_android
-- Tablas: routes, stops, sync_log


SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE IF NOT EXISTS `routes` (
    `id`            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `account_id`    INT UNSIGNED NOT NULL,
    `user_id`       INT UNSIGNED NOT NULL,
    `uid`           VARCHAR(36)  NOT NULL UNIQUE,
    `name`          VARCHAR(255) NOT NULL,
    `date_assigned` DATE         NOT NULL,
    `status`        ENUM('pending','active','done','cancelled') NOT NULL DEFAULT 'pending',
    `notes`         TEXT         DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`    DATETIME     DEFAULT NULL,
    INDEX `idx_account_date` (`account_id`, `date_assigned`),
    INDEX `idx_user_date`    (`user_id`,    `date_assigned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `stops` (
    `id`          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `route_id`    INT UNSIGNED NOT NULL,
    `uid`         VARCHAR(36)  NOT NULL UNIQUE,
    `account_id`  INT UNSIGNED NOT NULL,
    `name`        VARCHAR(255) NOT NULL,
    `address`     VARCHAR(500) DEFAULT NULL,
    `lat`         DECIMAL(10,7) DEFAULT NULL,
    `lng`         DECIMAL(10,7) DEFAULT NULL,
    `order_index` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    `status`      ENUM('pending','visiting','done','skipped') NOT NULL DEFAULT 'pending',
    `notes`       TEXT         DEFAULT NULL,
    `visited_at`  DATETIME     DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`  DATETIME     DEFAULT NULL,
    INDEX `idx_route`   (`route_id`),
    INDEX `idx_account` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sync_log` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `account_id` INT UNSIGNED NOT NULL,
    `user_id`    INT UNSIGNED NOT NULL,
    `entity`     VARCHAR(30)  NOT NULL,
    `entity_uid` VARCHAR(36)  NOT NULL,
    `operation`  ENUM('create','update','delete') NOT NULL,
    `synced_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_account_time` (`account_id`, `synced_at`),
    INDEX `idx_user_time`    (`user_id`,    `synced_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `schema_migrations` (`version`) VALUES ('v2.0.0');

SET FOREIGN_KEY_CHECKS=1;
