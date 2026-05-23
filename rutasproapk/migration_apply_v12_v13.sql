-- ══════════════════════════════════════════════════════════════
-- MIGRACIONES v12 + v13 — sin INFORMATION_SCHEMA
-- Compatible con hosting compartido (cPanel, Plesk, etc.)
-- Ejecutar en MySQL como:
--   mysql -u user -p db_name < migration_apply_v12_v13.sql
-- ══════════════════════════════════════════════════════════════

-- Tabla de control de migraciones aplicadas
CREATE TABLE IF NOT EXISTS schema_migrations (
    version     INT          NOT NULL PRIMARY KEY,
    applied_at  DATETIME     NOT NULL DEFAULT NOW(),
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── v12: check-in/out timestamps + GPS de visita ─────────────
-- ALTER TABLE IGNORE no existe en MySQL — usamos stored procedure
DROP PROCEDURE IF EXISTS apply_migration_v12;
DELIMITER $$
CREATE PROCEDURE apply_migration_v12()
BEGIN
    -- check_in_ts
    IF NOT EXISTS (SELECT 1 FROM schema_migrations WHERE version = 12) THEN
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLSTATE '42S21' BEGIN END; -- Duplicate column
            ALTER TABLE stops ADD COLUMN check_in_ts BIGINT NULL
                COMMENT 'Epoch ms — cuando el agente abrió el formulario';
        END;
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLSTATE '42S21' BEGIN END;
            ALTER TABLE stops ADD COLUMN check_out_ts BIGINT NULL
                COMMENT 'Epoch ms — cuando se guardó la visita';
        END;
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLSTATE '42S21' BEGIN END;
            ALTER TABLE stops ADD COLUMN gps_lat_visit DOUBLE NULL
                COMMENT 'Latitud GPS del agente al check-in';
        END;
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLSTATE '42S21' BEGIN END;
            ALTER TABLE stops ADD COLUMN gps_lng_visit DOUBLE NULL
                COMMENT 'Longitud GPS del agente al check-in';
        END;
        INSERT IGNORE INTO schema_migrations (version, description)
        VALUES (12, 'check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit en stops');
        SELECT 'v12 aplicada' AS resultado;
    ELSE
        SELECT 'v12 ya estaba aplicada' AS resultado;
    END IF;
END$$
DELIMITER ;

CALL apply_migration_v12();
DROP PROCEDURE IF EXISTS apply_migration_v12;

-- ── v13: date_assigned en stops ───────────────────────────────
DROP PROCEDURE IF EXISTS apply_migration_v13;
DELIMITER $$
CREATE PROCEDURE apply_migration_v13()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM schema_migrations WHERE version = 13) THEN
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLSTATE '42S21' BEGIN END;
            ALTER TABLE stops ADD COLUMN date_assigned DATE NULL
                COMMENT 'Fecha de esta visita concreta — 1 stop por fecha por PDV';
        END;
        INSERT IGNORE INTO schema_migrations (version, description)
        VALUES (13, 'date_assigned en stops');
        SELECT 'v13 aplicada' AS resultado;
    ELSE
        SELECT 'v13 ya estaba aplicada' AS resultado;
    END IF;
END$$
DELIMITER ;

CALL apply_migration_v13();
DROP PROCEDURE IF EXISTS apply_migration_v13;

-- ── manager_id en users ───────────────────────────────────────
DROP PROCEDURE IF EXISTS apply_migration_mgr;
DELIMITER $$
CREATE PROCEDURE apply_migration_mgr()
BEGIN
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLSTATE '42S21' BEGIN END;
        ALTER TABLE users ADD COLUMN manager_id INT NULL
            COMMENT 'FK → users.id del supervisor directo';
    END;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
        ALTER TABLE users ADD INDEX idx_users_manager (manager_id);
    END;
    SELECT 'manager_id listo' AS resultado;
END$$
DELIMITER ;

CALL apply_migration_mgr();
DROP PROCEDURE IF EXISTS apply_migration_mgr;

-- ── Verificar resultado ───────────────────────────────────────
SELECT version, applied_at, description FROM schema_migrations ORDER BY version;
SHOW COLUMNS FROM stops LIKE 'check_in_ts';
SHOW COLUMNS FROM stops LIKE 'check_out_ts';
SHOW COLUMNS FROM stops LIKE 'gps_lat_visit';
SHOW COLUMNS FROM stops LIKE 'gps_lng_visit';
SHOW COLUMNS FROM stops LIKE 'date_assigned';
SHOW COLUMNS FROM users LIKE 'manager_id';
