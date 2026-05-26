-- ══════════════════════════════════════════════════════════════
-- SEED DEMO — Jerarquía completa + ruta de 5 paradas
-- Contraseña para todos: God2026!
-- Hash: $2y$12$3Von/mnKGGcD7rclEp9IpegDiSu0mFSVGuHgFAfNBiIvZGECYWW3W
--
-- IDs: account=10, owner=20, admin=21, manager=22, agent=23
--      route=50, stops=100-104
--
-- Ejecutar en phpMyAdmin sobre cqvkelal_rutasapp_android
-- ══════════════════════════════════════════════════════════════

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ──────────────────────────────────────────────────────────────
-- 1. ACCOUNT — empresa de distribución
-- ──────────────────────────────────────────────────────────────

INSERT INTO `accounts`
    (id, type, name, slug, plan, plus_config, form_config, ai_settings)
VALUES (10, 'company', 'Distribuciones Norte SL', 'distribuciones-norte-sl', 'business',
    NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name);


-- ──────────────────────────────────────────────────────────────
-- 2. USUARIOS — jerarquía completa
--    owner → admin (manager_id=owner) → manager (manager_id=admin) → agent (manager_id=manager)
-- ──────────────────────────────────────────────────────────────

-- OWNER — propietario de la empresa
INSERT INTO `users`
    (id, account_id, username, email, password_hash, name, role, manager_id, active)
VALUES (20, 10, 'demo_owner', 'owner@demo.com',
    '$2y$12$3Von/mnKGGcD7rclEp9IpegDiSu0mFSVGuHgFAfNBiIvZGECYWW3W', 'Laura Martínez (Owner)', 'owner', NULL, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ADMIN — jefe territorial, reporta a owner
INSERT INTO `users`
    (id, account_id, username, email, password_hash, name, role, manager_id, active)
VALUES (21, 10, 'demo_admin', 'admin@demo.com',
    '$2y$12$3Von/mnKGGcD7rclEp9IpegDiSu0mFSVGuHgFAfNBiIvZGECYWW3W', 'Carlos Ruiz (Admin)', 'admin', 20, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- MANAGER — supervisor de zona, reporta a admin
INSERT INTO `users`
    (id, account_id, username, email, password_hash, name, role, manager_id, active)
VALUES (22, 10, 'demo_manager', 'manager@demo.com',
    '$2y$12$3Von/mnKGGcD7rclEp9IpegDiSu0mFSVGuHgFAfNBiIvZGECYWW3W', 'Ana Gómez (Manager)', 'manager', 21, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- AGENT — comercial en campo, reporta a manager
INSERT INTO `users`
    (id, account_id, username, email, password_hash, name, role, manager_id, active)
VALUES (23, 10, 'demo_agent', 'agent@demo.com',
    '$2y$12$3Von/mnKGGcD7rclEp9IpegDiSu0mFSVGuHgFAfNBiIvZGECYWW3W', 'Javier López (Agent)', 'agent', 22, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);


-- ──────────────────────────────────────────────────────────────
-- 3. PERFIL DE NEGOCIO — sector distribución
-- ──────────────────────────────────────────────────────────────

INSERT INTO `business_profiles`
    (account_id, sector, name, updated_at)
VALUES (10, 'distribucion', 'Distribuciones Norte SL', UNIX_TIMESTAMP())
ON DUPLICATE KEY UPDATE sector = VALUES(sector), name = VALUES(name);


-- ──────────────────────────────────────────────────────────────
-- 4. RUTA — asignada al agente para hoy
-- ──────────────────────────────────────────────────────────────

INSERT INTO `routes`
    (id, account_id, user_id, uid, name, date_assigned, status, notes,
     scheduled_dates, created_at, updated_at, deleted_at)
VALUES (
    50, 10, 23,
    'demo-ruta-00000000-0000-0000-0001',
    'Ruta Valencia Centro — Demo',
    '2026-05-26',
    'active',
    'Ruta de demostración con 5 PDVs de distribución. Objetivo: cerrar pedido mínimo 500€/PDV.',
    NULL,
    '2026-05-26 08:00:00', '2026-05-26 08:00:00', NULL
)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status);


-- ──────────────────────────────────────────────────────────────
-- 5. PARADAS — 5 PDVs con todos los campos rellenos
-- ──────────────────────────────────────────────────────────────

-- Stop 1: PDV visitado y contactado
INSERT INTO `stops`
    (id, route_id, uid, account_id, name, address, lat, lng, order_index,
     external_id, contact_name, contact_phone,
     visit_frequency, priority, segment, account_status, opening_hours,
     pdv_open, pdv_inactive,
     status, notes, visited_at, visit_result, next_action,
     date_assigned, check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
     created_at, updated_at, deleted_at)
VALUES (
    100, 50, 'demo-stop-00000000-0000-0000-0001', 10,
    'Supermercado El Huerto',
    'Calle Colón 12, Valencia',
    39.4699, -0.3763,
    0,
    'PDV001',
    'María Torres', '+34 963 111 222',
    7, 1, 'A', 'active',
    '{"mon":"09:00-20:00","tue":"09:00-20:00","wed":"09:00-20:00","thu":"09:00-20:00","fri":"09:00-20:00","sat":"10:00-14:00","sun":null}',
    1, 0,
    'done', 'Cliente satisfecho. Amplió pedido de bebidas.',
    '2026-05-26 09:15:00', 'contactado', 'Llamar el próximo lunes para confirmar entrega.',
    '2026-05-26',
    1748247300000, 1748247960000,
    39.4700, -0.3764,
    '2026-05-26 08:00:00', '2026-05-26 09:36:00', NULL
)
ON DUPLICATE KEY UPDATE status = VALUES(status), visited_at = VALUES(visited_at);

-- Stop 2: PDV pendiente
INSERT INTO `stops`
    (id, route_id, uid, account_id, name, address, lat, lng, order_index,
     external_id, contact_name, contact_phone,
     visit_frequency, priority, segment, account_status, opening_hours,
     pdv_open, pdv_inactive,
     status, notes, visited_at, visit_result, next_action,
     date_assigned, check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
     created_at, updated_at, deleted_at)
VALUES (
    101, 50, 'demo-stop-00000000-0000-0000-0002', 10,
    'Tienda La Fresquera',
    'Avenida del Puerto 45, Valencia',
    39.4712, -0.3658,
    1,
    'PDV002',
    'Antonio García', '+34 963 222 333',
    14, 2, 'B', 'active',
    '{"mon":"08:00-14:00","tue":"08:00-14:00","wed":"08:00-14:00","thu":"08:00-14:00","fri":"08:00-14:00","sat":null,"sun":null}',
    1, 0,
    'pending', 'Revisar stock de refrescos. Suelen necesitar reposición los lunes.',
    NULL, NULL, NULL,
    '2026-05-26',
    NULL, NULL, NULL, NULL,
    '2026-05-26 08:00:00', '2026-05-26 08:00:00', NULL
)
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- Stop 3: PDV saltado (no estaba)
INSERT INTO `stops`
    (id, route_id, uid, account_id, name, address, lat, lng, order_index,
     external_id, contact_name, contact_phone,
     visit_frequency, priority, segment, account_status, opening_hours,
     pdv_open, pdv_inactive,
     status, notes, visited_at, visit_result, next_action,
     date_assigned, check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
     created_at, updated_at, deleted_at)
VALUES (
    102, 50, 'demo-stop-00000000-0000-0000-0003', 10,
    'Bar Deportivo Mestalla',
    'Calle Mestalla 8, Valencia',
    39.4747, -0.3586,
    2,
    'PDV003',
    'Roberto Sanz', '+34 963 333 444',
    30, 3, 'C', 'active',
    '{"mon":"07:00-23:00","tue":"07:00-23:00","wed":"07:00-23:00","thu":"07:00-23:00","fri":"07:00-01:00","sat":"07:00-01:00","sun":"07:00-23:00"}',
    0, 0,
    'skipped',
    'Cerrado por obras. Reabren en 2 semanas según cartel.',
    '2026-05-26 10:45:00', 'no_estaba', 'Volver en 2 semanas cuando terminen obras.',
    '2026-05-26',
    1748251500000, 1748251620000,
    39.4748, -0.3587,
    '2026-05-26 08:00:00', '2026-05-26 10:47:00', NULL
)
ON DUPLICATE KEY UPDATE status = VALUES(status), visited_at = VALUES(visited_at);

-- Stop 4: PDV pendiente, alta prioridad
INSERT INTO `stops`
    (id, route_id, uid, account_id, name, address, lat, lng, order_index,
     external_id, contact_name, contact_phone,
     visit_frequency, priority, segment, account_status, opening_hours,
     pdv_open, pdv_inactive,
     status, notes, visited_at, visit_result, next_action,
     date_assigned, check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
     created_at, updated_at, deleted_at)
VALUES (
    103, 50, 'demo-stop-00000000-0000-0000-0004', 10,
    'Carnicería Hermanos Pérez',
    'Mercado Central, Puesto 34, Valencia',
    39.4736, -0.3790,
    3,
    'PDV004',
    'Luis Pérez', '+34 963 444 555',
    7, 1, 'A', 'active',
    '{"mon":"08:00-15:00","tue":"08:00-15:00","wed":"08:00-15:00","thu":"08:00-15:00","fri":"08:00-15:00","sat":"08:00-14:00","sun":null}',
    1, 0,
    'pending', 'Cliente premium. Suele hacer pedidos grandes los lunes. No saltarse.',
    NULL, NULL, NULL,
    '2026-05-26',
    NULL, NULL, NULL, NULL,
    '2026-05-26 08:00:00', '2026-05-26 08:00:00', NULL
)
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- Stop 5: PDV inactivo (cerrado permanentemente)
INSERT INTO `stops`
    (id, route_id, uid, account_id, name, address, lat, lng, order_index,
     external_id, contact_name, contact_phone,
     visit_frequency, priority, segment, account_status, opening_hours,
     pdv_open, pdv_inactive,
     status, notes, visited_at, visit_result, next_action,
     date_assigned, check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
     created_at, updated_at, deleted_at)
VALUES (
    104, 50, 'demo-stop-00000000-0000-0000-0005', 10,
    'Quiosco Jardines Turia',
    'Jardines del Turia, frente al Palau, Valencia',
    39.4693, -0.3756,
    4,
    'PDV005',
    'Carmen Blanco', '+34 963 555 666',
    30, 4, 'C', 'inactive',
    '{"mon":"10:00-20:00","tue":"10:00-20:00","wed":"10:00-20:00","thu":"10:00-20:00","fri":"10:00-20:00","sat":"10:00-20:00","sun":"10:00-20:00"}',
    0, 1,
    'skipped', 'PDV cerrado definitivamente. Traspaso del local pendiente de formalizar.',
    '2026-05-26 11:30:00', 'rechazado', 'Dar de baja en el sistema. Notificar a admin.',
    '2026-05-26',
    1748254200000, 1748254380000,
    39.4694, -0.3757,
    '2026-05-26 08:00:00', '2026-05-26 11:33:00', NULL
)
ON DUPLICATE KEY UPDATE status = VALUES(status), pdv_inactive = VALUES(pdv_inactive);


-- ──────────────────────────────────────────────────────────────
-- 6. JORNADA — sesión activa del agente hoy
-- ──────────────────────────────────────────────────────────────

INSERT INTO `day_sessions`
    (account_id, user_id, route_uid, date_str, state,
     started_at, paused_at, elapsed_ms, distance_km,
     last_lat, last_lng, updated_at)
VALUES (
    10, 23,
    'demo-ruta-00000000-0000-0000-0001',
    '2026-05-26',
    'running',
    1748245800000,
    NULL,
    9360000,
    4.720,
    39.4748, -0.3587,
    UNIX_TIMESTAMP() * 1000
)
ON DUPLICATE KEY UPDATE
    state      = VALUES(state),
    elapsed_ms = VALUES(elapsed_ms),
    distance_km = VALUES(distance_km);


-- ──────────────────────────────────────────────────────────────
-- 7. KPI VALUES — registros de la visita al Stop 1
-- ──────────────────────────────────────────────────────────────

INSERT IGNORE INTO `kpi_values`
    (account_id, stop_uid, kpi_id, value_text)
VALUES
(10, 'demo-stop-00000000-0000-0000-0001', 'common_resultado',  'contactado'),
(10, 'demo-stop-00000000-0000-0000-0001', 'common_duracion',   '21'),
(10, 'demo-stop-00000000-0000-0000-0001', 'common_notas',      'Cliente muy receptivo. Stock bajo en agua mineral.'),
(10, 'demo-stop-00000000-0000-0000-0001', 'common_next_action','Llamar lunes para confirmar entrega'),
(10, 'demo-stop-00000000-0000-0000-0001', 'dist_pedido_eur',   '780'),
(10, 'demo-stop-00000000-0000-0000-0001', 'dist_referencias',  '12'),
(10, 'demo-stop-00000000-0000-0000-0001', 'dist_exposicion',   'true'),
(10, 'demo-stop-00000000-0000-0000-0001', 'dist_incidencias',  '0');

-- KPI Values stop 3 (saltado)
INSERT IGNORE INTO `kpi_values`
    (account_id, stop_uid, kpi_id, value_text)
VALUES
(10, 'demo-stop-00000000-0000-0000-0003', 'common_resultado',  'no_estaba'),
(10, 'demo-stop-00000000-0000-0000-0003', 'common_notas',      'Cerrado por obras. Cartel: reapertura 2 semanas.'),
(10, 'demo-stop-00000000-0000-0000-0003', 'common_next_action','Volver en 2 semanas');


-- ──────────────────────────────────────────────────────────────
-- 8. KPI DEFINITIONS — catálogo distribución (si no existe ya)
-- ──────────────────────────────────────────────────────────────

INSERT IGNORE INTO `kpi_definitions`
    (id, account_id, sector, label, type, unit, options, required, visible, order_index, section, is_system)
VALUES
('common_resultado',  0,'common','Resultado visita','select',NULL,'["contactado","no_estaba","volvemos","rechazado"]',1,1,0,'general',1),
('common_duracion',   0,'common','Duración (min)','number','min',NULL,0,1,1,'general',1),
('common_notas',      0,'common','Notas','text',NULL,NULL,0,1,0,'notas',1),
('common_next_action',0,'common','Próxima acción','text',NULL,NULL,0,1,1,'notas',1),
('dist_pedido_eur',   0,'distribucion','Pedido (€)','number','€',NULL,1,1,0,'pedidos',1),
('dist_referencias',  0,'distribucion','Refs. pedidas','number','ud',NULL,0,1,1,'pedidos',1),
('dist_incidencias',  0,'distribucion','Incidencias logíst.','number','ud',NULL,0,1,0,'general',1),
('dist_exposicion',   0,'distribucion','Exposición OK','boolean',NULL,NULL,0,1,0,'objetivos',1),
('dist_competencia',  0,'distribucion','Acción competencia','text',NULL,NULL,0,0,2,'notas',1);


-- ──────────────────────────────────────────────────────────────
-- 9. USER_PREFS — prefs vacías para los 4 usuarios
-- ──────────────────────────────────────────────────────────────

INSERT IGNORE INTO `user_prefs` (user_id, prefs)
VALUES
(20, '{}'),
(21, '{}'),
(22, '{}'),
(23, '{}');


SET FOREIGN_KEY_CHECKS = 1;

-- ──────────────────────────────────────────────────────────────
-- VERIFICACIÓN
-- ──────────────────────────────────────────────────────────────
SELECT
    u.id, u.username, u.role, u.manager_id,
    sup.username AS supervisor,
    a.name       AS account
FROM users u
JOIN accounts a ON a.id = u.account_id
LEFT JOIN users sup ON sup.id = u.manager_id
WHERE u.account_id = 10
ORDER BY u.id;

SELECT id, name, status, date_assigned FROM routes WHERE account_id = 10;
SELECT id, name, status, date_assigned, pdv_open, pdv_inactive FROM stops WHERE route_id = 50 ORDER BY order_index;
