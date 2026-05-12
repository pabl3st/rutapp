-- migration_v10.sql — tabla visit_photos en servidor
-- Aplicar en phpMyAdmin sobre cqvkelal_rutasapp_android

CREATE TABLE IF NOT EXISTS `visit_photos` (
    `id`         INT NOT NULL AUTO_INCREMENT,
    `account_id` INT NOT NULL,
    `user_id`    INT NOT NULL,
    `stop_uid`   VARCHAR(36) NOT NULL,
    `photo_uid`  VARCHAR(36) NOT NULL,       -- uid local de VisitPhotoEntity
    `file_path`  VARCHAR(500) NOT NULL,       -- path en disco del servidor
    `file_url`   VARCHAR(500) NOT NULL,       -- URL pública para descargar
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_photo_uid` (`photo_uid`),  -- idempotente: re-upload mismo uid no duplica
    KEY `idx_vp_stop`    (`stop_uid`),
    KEY `idx_vp_account` (`account_id`),
    CONSTRAINT `fk_vp_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `schema_migrations` (`version`) VALUES ('v10.0.0');
