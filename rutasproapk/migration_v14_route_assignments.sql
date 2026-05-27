-- migration_v14_route_assignments.sql
-- Log append-only de reasignaciones de ruta. Nunca se hace UPDATE/DELETE aquí.
-- Es un registro de auditoría: guarda nombres desnormalizados para seguir
-- siendo legible aunque un usuario se borre o renombre.
CREATE TABLE IF NOT EXISTS route_assignments (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id       INT             NOT NULL,
    route_uid        VARCHAR(50)     NOT NULL,
    route_name       VARCHAR(120)    NOT NULL,            -- desnormalizado: legible aunque la ruta se borre
    from_user_id     INT             NULL,                -- NULL = la ruta no tenía asignación previa
    from_user_name   VARCHAR(120)    NULL,
    to_user_id       INT             NOT NULL,
    to_user_name     VARCHAR(120)    NOT NULL,
    assigned_by_id   INT             NOT NULL,            -- quién ejecutó la reasignación
    assigned_by_name VARCHAR(120)    NOT NULL,
    reason           VARCHAR(255)    NULL,                -- motivo opcional
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_route   (route_uid),
    INDEX idx_account (account_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
