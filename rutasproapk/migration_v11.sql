-- migration_v11.sql — tabla push_tokens para FCM multi-dispositivo
-- Aplicar en phpMyAdmin sobre cqvkelal_rutasapp_android

SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE IF NOT EXISTS `push_tokens` (
    `id`          INT NOT NULL AUTO_INCREMENT,
    `user_id`     INT NOT NULL,
    `account_id`  INT NOT NULL,
    `fcm_token`   VARCHAR(4096) NOT NULL,
    `device_id`   VARCHAR(64)   NOT NULL,
    `device_name` VARCHAR(128)  DEFAULT NULL,
    `platform`    VARCHAR(16)   NOT NULL DEFAULT 'android',
    `app_version` VARCHAR(32)   DEFAULT NULL,
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_device` (`user_id`, `device_id`),
    KEY `idx_pt_user`    (`user_id`),
    KEY `idx_pt_account` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS=1;

INSERT IGNORE INTO `schema_migrations` (`version`) VALUES ('v11.0.0');
