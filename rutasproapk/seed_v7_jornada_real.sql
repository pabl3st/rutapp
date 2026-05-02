-- ============================================================
-- seed_v7_jornada_real.sql
-- Jornada realista desde Plaza del Ayuntamiento de Valencia
-- Ayer (01-may): jornada completa, todas las visitas hechas
-- Hoy (02-may):  todas hechas menos la última (para test)
-- Mañana (03-may): todo vacío/pending
-- Aplicar DESPUÉS de seed_v6_testdata.sql
-- ============================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================================
-- LIMPIEZA — eliminar datos de prueba inconsistentes
-- ============================================================

-- Eliminar jornadas antiguas de day_sessions para user_id=2
DELETE FROM `day_sessions` WHERE user_id = 2;

-- Eliminar kpi_values de stops que vamos a resetear
DELETE FROM `kpi_values` WHERE account_id = 2;

-- Resetear todos los stops existentes de rutas del usuario 2
UPDATE `stops` SET
    status       = 'pending',
    visit_result = NULL,
    notes        = NULL,
    next_action  = NULL,
    visited_at   = NULL
WHERE account_id = 2;

-- ============================================================
-- RUTA AYER (01-may-2026): Ruta Centro Valencia
-- Salida: Plaza del Ayuntamiento (39.4699, -0.3763)
-- 6 paradas en orden geográfico por cercanía en coche
-- Jornada: 09:00 → 18:30, 12.4 km recorridos
-- ============================================================

-- Asegurar que la ruta de ayer existe y está done
INSERT INTO `routes`
    (id, account_id, user_id, uid, name, date_assigned, status, notes, created_at, updated_at)
VALUES
(1, 2, 2, '550e8400-e29b-41d4-a716-446655440001',
 'Ruta Centro Valencia', '2026-05-01', 'done',
 'Zona centro — salida desde Plaza Ayuntamiento', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name='Ruta Centro Valencia', date_assigned='2026-05-01',
    status='done', updated_at=NOW();

-- Borrar stops anteriores de esta ruta y recrear con orden geográfico
DELETE FROM `stops` WHERE route_id = 1 AND account_id = 2;

INSERT INTO `stops`
(route_id, uid, account_id, name, address,
 lat, lng, order_index,
 external_id, contact_name, contact_phone,
 visit_frequency, priority, segment, account_status, opening_hours,
 status, notes, visited_at, visit_result, next_action,
 created_at, updated_at)
VALUES

-- Parada 0: Distribuciones Martínez — Calle Colón 12
-- 09:15 llegada, 09:40 salida (25 min). 0.8km desde Ayuntamiento
(1, 'aaaa0001-0000-0000-0000-000000000001', 2,
 'Distribuciones Martínez', 'Calle Colón 12, Valencia',
 39.4703, -0.3697, 0,
 'DIST-00141', 'Ramón Martínez', '+34 963 410 077',
 30, 2, 'A', 'active',
 '{"mon":"09:00-14:00,16:00-19:00","tue":"09:00-14:00,16:00-19:00","wed":"09:00-14:00,16:00-19:00","thu":"09:00-14:00,16:00-19:00","fri":"09:00-14:00"}',
 'done', 'Pedir albarán firmado. Muy receptivo.',
 '2026-05-01 09:15:00', 'contactado',
 'Enviar propuesta fibra empresa 300Mb — precio acordado 89€/mes',
 NOW(), '2026-05-01 09:40:00'),

-- Parada 1: Electrónica Pérez — Calle Jorge Juan 28
-- 09:55 llegada, 10:35 salida (40 min). 1.2km desde anterior
(1, 'aaaa0001-0000-0000-0000-000000000002', 2,
 'Electrónica Pérez', 'Calle Jorge Juan 28, Valencia',
 39.4721, -0.3651, 1,
 'ELEC-00287', 'Sonia Pérez', '+34 963 528 901',
 14, 1, 'A', 'active',
 '{"mon":"10:00-14:00,17:00-20:00","tue":"10:00-14:00,17:00-20:00","wed":"10:00-14:00,17:00-20:00","thu":"10:00-14:00,17:00-20:00","fri":"10:00-14:00,17:00-20:00","sat":"10:00-14:00"}',
 'done', 'Renovar contrato fibra. Firmará la semana que viene.',
 '2026-05-01 09:55:00', 'contactado',
 'Firmar contrato fibra + 2 móviles el próximo jueves',
 NOW(), '2026-05-01 10:35:00'),

-- Parada 2: Talleres García — Calle Cuarte 88
-- 11:00 llegada, 11:30 salida (30 min). 1.5km desde anterior
(1, 'aaaa0001-0000-0000-0000-000000000004', 2,
 'Talleres García', 'Calle Cuarte 88, Valencia',
 39.4676, -0.3821, 2,
 'TALL-00019', 'Paco García', '+34 961 734 512',
 30, 2, 'B', 'active',
 '{"mon":"08:00-18:00","tue":"08:00-18:00","wed":"08:00-18:00","thu":"08:00-18:00","fri":"08:00-14:00"}',
 'done', 'Visita técnica instalación. Instalador confirmado para lunes.',
 '2026-05-01 11:00:00', 'contactado',
 'Revisar factura anterior — posible error cobro doble',
 NOW(), '2026-05-01 11:30:00'),

-- PAUSA COMIDA 12:00-13:30

-- Parada 3: Academia de Idiomas Oxford — Gran Vía 24
-- 13:45 llegada, 14:30 salida (45 min). 1.1km desde anterior
(1, 'aaaa0001-0000-0000-0000-000000000006', 2,
 'Academia de Idiomas Oxford', 'Gran Vía Marqués del Turia 24, Valencia',
 39.4691, -0.3758, 3,
 'ACAD-00031', 'Carlos Ribera', '+34 963 672 445',
 60, 1, 'A', 'active',
 '{"mon":"09:00-21:00","tue":"09:00-21:00","wed":"09:00-21:00","thu":"09:00-21:00","fri":"09:00-14:00","sat":"09:00-14:00"}',
 'done', 'Contrato firmado — 3 líneas. Quiere ampliar en septiembre.',
 '2026-05-01 13:45:00', 'contactado',
 'Ampliar a 5 líneas en septiembre — confirmar en julio',
 NOW(), '2026-05-01 14:30:00'),

-- Parada 4: Clínica Dental Noguera — Calle Cirilo Amorós 15
-- 15:00 llegada, 15:20 salida (20 min). 0.9km desde anterior
(1, 'aaaa0001-0000-0000-0000-000000000005', 2,
 'Clínica Dental Noguera', 'Calle Cirilo Amorós 15, Valencia',
 39.4679, -0.3728, 4,
 'CLIN-00076', 'Dra. Laura Noguera', '+34 963 841 200',
 30, 1, 'A', 'active',
 '{"mon":"09:00-14:00,16:00-20:00","tue":"09:00-14:00,16:00-20:00","wed":"09:00-14:00","thu":"09:00-14:00,16:00-20:00","fri":"09:00-14:00"}',
 'done', 'Alta nueva línea móvil para la recepcionista.',
 '2026-05-01 15:00:00', 'contactado',
 'Alta nueva línea confirmada — enviar SIM por correo',
 NOW(), '2026-05-01 15:20:00'),

-- Parada 5: Bar Restaurante El Pilar — Calle del Pilar 9
-- 16:00 llegada, 16:10 salida (10 min). Cerrado por descanso semanal.
(1, 'aaaa0001-0000-0000-0000-000000000003', 2,
 'Bar Restaurante El Pilar', 'Calle del Pilar 9, Valencia',
 39.4733, -0.3772, 5,
 'BAR-00053', 'Miguel Andreu', '+34 963 918 332',
 60, 3, 'B', 'active',
 '{"mon":"08:00-16:00","tue":"08:00-16:00","wed":"08:00-16:00","thu":"08:00-16:00","fri":"08:00-23:00","sat":"09:00-23:00","sun":"09:00-16:00"}',
 'done', 'Cerrado por descanso semanal — jueves cierra.',
 '2026-05-01 16:00:00', 'no_estaba',
 'Visitar el viernes o sábado — mejor por la mañana',
 NOW(), '2026-05-01 16:10:00');

-- ============================================================
-- DAY_SESSION AYER — jornada completa 09:00-18:30
-- Salida: Plaza Ayuntamiento Valencia (39.4699, -0.3763)
-- started_at: 2026-05-01 09:00:00 UTC+2 = 07:00:00 UTC = 1746086400000 ms
-- elapsed: 9.5 horas = 34200000 ms
-- ============================================================

INSERT INTO `day_sessions`
    (account_id, user_id, route_uid, date_str, state,
     started_at, elapsed_ms, distance_km,
     last_lat, last_lng, updated_at)
VALUES
(2, 2, '550e8400-e29b-41d4-a716-446655440001', '2026-05-01',
 'done',
 1746086400000,  -- 2026-05-01 07:00:00 UTC (09:00 Valencia)
 34200000,       -- 9h30m de jornada
 12.400,
 39.4733, -0.3772,  -- última posición: El Pilar
 1746120600000); -- 2026-05-01 18:30:00 Valencia

-- ============================================================
-- KPI_VALUES AYER — formularios completos, sector telco
-- ============================================================

INSERT INTO `kpi_values` (account_id, stop_uid, kpi_id, value_text) VALUES

-- Parada 0: Distribuciones Martínez
(2,'aaaa0001-0000-0000-0000-000000000001','common_resultado',    'contactado'),
(2,'aaaa0001-0000-0000-0000-000000000001','common_duracion',     '25'),
(2,'aaaa0001-0000-0000-0000-000000000001','telco_activaciones',  '2'),
(2,'aaaa0001-0000-0000-0000-000000000001','telco_bono_eur',      '178.00'),
(2,'aaaa0001-0000-0000-0000-000000000001','telco_renovaciones',  '1'),
(2,'aaaa0001-0000-0000-0000-000000000001','telco_portabilidades','0'),
(2,'aaaa0001-0000-0000-0000-000000000001','telco_churns',        '0'),
(2,'aaaa0001-0000-0000-0000-000000000001','telco_stock_sims',    '10'),
(2,'aaaa0001-0000-0000-0000-000000000001','telco_plus',          'false'),
(2,'aaaa0001-0000-0000-0000-000000000001','common_notas',        'Pedir albarán firmado. Muy receptivo.'),
(2,'aaaa0001-0000-0000-0000-000000000001','common_next_action',  'Enviar propuesta fibra empresa 300Mb'),

-- Parada 1: Electrónica Pérez
(2,'aaaa0001-0000-0000-0000-000000000002','common_resultado',    'contactado'),
(2,'aaaa0001-0000-0000-0000-000000000002','common_duracion',     '40'),
(2,'aaaa0001-0000-0000-0000-000000000002','telco_activaciones',  '3'),
(2,'aaaa0001-0000-0000-0000-000000000002','telco_bono_eur',      '267.00'),
(2,'aaaa0001-0000-0000-0000-000000000002','telco_renovaciones',  '2'),
(2,'aaaa0001-0000-0000-0000-000000000002','telco_portabilidades','1'),
(2,'aaaa0001-0000-0000-0000-000000000002','telco_churns',        '0'),
(2,'aaaa0001-0000-0000-0000-000000000002','telco_stock_sims',    '15'),
(2,'aaaa0001-0000-0000-0000-000000000002','telco_plus',          'true'),
(2,'aaaa0001-0000-0000-0000-000000000002','common_notas',        'Renovar contrato fibra. Firmará la semana que viene.'),
(2,'aaaa0001-0000-0000-0000-000000000002','common_next_action',  'Firmar contrato fibra + 2 móviles el próximo jueves'),

-- Parada 2: Talleres García
(2,'aaaa0001-0000-0000-0000-000000000004','common_resultado',    'contactado'),
(2,'aaaa0001-0000-0000-0000-000000000004','common_duracion',     '30'),
(2,'aaaa0001-0000-0000-0000-000000000004','telco_activaciones',  '1'),
(2,'aaaa0001-0000-0000-0000-000000000004','telco_bono_eur',      '89.00'),
(2,'aaaa0001-0000-0000-0000-000000000004','telco_renovaciones',  '0'),
(2,'aaaa0001-0000-0000-0000-000000000004','telco_portabilidades','0'),
(2,'aaaa0001-0000-0000-0000-000000000004','telco_churns',        '1'),
(2,'aaaa0001-0000-0000-0000-000000000004','telco_stock_sims',    '5'),
(2,'aaaa0001-0000-0000-0000-000000000004','telco_plus',          'false'),
(2,'aaaa0001-0000-0000-0000-000000000004','common_notas',        'Visita técnica instalación. Instalador confirmado para lunes.'),
(2,'aaaa0001-0000-0000-0000-000000000004','common_next_action',  'Revisar factura anterior — posible error cobro doble'),

-- Parada 3: Academia Oxford
(2,'aaaa0001-0000-0000-0000-000000000006','common_resultado',    'contactado'),
(2,'aaaa0001-0000-0000-0000-000000000006','common_duracion',     '45'),
(2,'aaaa0001-0000-0000-0000-000000000006','telco_activaciones',  '3'),
(2,'aaaa0001-0000-0000-0000-000000000006','telco_bono_eur',      '267.00'),
(2,'aaaa0001-0000-0000-0000-000000000006','telco_renovaciones',  '3'),
(2,'aaaa0001-0000-0000-0000-000000000006','telco_portabilidades','2'),
(2,'aaaa0001-0000-0000-0000-000000000006','telco_churns',        '0'),
(2,'aaaa0001-0000-0000-0000-000000000006','telco_stock_sims',    '0'),
(2,'aaaa0001-0000-0000-0000-000000000006','telco_plus',          'true'),
(2,'aaaa0001-0000-0000-0000-000000000006','common_notas',        'Contrato firmado — 3 líneas. Quiere ampliar en septiembre.'),
(2,'aaaa0001-0000-0000-0000-000000000006','common_next_action',  'Ampliar a 5 líneas en septiembre — confirmar en julio'),

-- Parada 4: Clínica Dental Noguera
(2,'aaaa0001-0000-0000-0000-000000000005','common_resultado',    'contactado'),
(2,'aaaa0001-0000-0000-0000-000000000005','common_duracion',     '20'),
(2,'aaaa0001-0000-0000-0000-000000000005','telco_activaciones',  '1'),
(2,'aaaa0001-0000-0000-0000-000000000005','telco_bono_eur',      '45.00'),
(2,'aaaa0001-0000-0000-0000-000000000005','telco_renovaciones',  '1'),
(2,'aaaa0001-0000-0000-0000-000000000005','telco_portabilidades','0'),
(2,'aaaa0001-0000-0000-0000-000000000005','telco_churns',        '0'),
(2,'aaaa0001-0000-0000-0000-000000000005','telco_stock_sims',    '3'),
(2,'aaaa0001-0000-0000-0000-000000000005','telco_plus',          'false'),
(2,'aaaa0001-0000-0000-0000-000000000005','common_notas',        'Alta nueva línea móvil para la recepcionista.'),
(2,'aaaa0001-0000-0000-0000-000000000005','common_next_action',  'Alta nueva línea confirmada — enviar SIM por correo'),

-- Parada 5: Bar El Pilar (no_estaba — formulario mínimo)
(2,'aaaa0001-0000-0000-0000-000000000003','common_resultado',    'no_estaba'),
(2,'aaaa0001-0000-0000-0000-000000000003','common_duracion',     '10'),
(2,'aaaa0001-0000-0000-0000-000000000003','common_notas',        'Cerrado por descanso semanal — jueves cierra.'),
(2,'aaaa0001-0000-0000-0000-000000000003','common_next_action',  'Visitar el viernes o sábado — mejor por la mañana');

-- ============================================================
-- RUTA HOY (02-may-2026): Ruta Extrarradio Valencia
-- 5 paradas — 4 hechas, 1 pending (para test del usuario)
-- Salida: Plaza Ayuntamiento → A-7 dirección Paterna
-- ============================================================

-- Ruta hoy ya existe (id=3) — actualizarla
UPDATE `routes` SET
    name         = 'Ruta Extrarradio Valencia',
    date_assigned = '2026-05-02',
    status       = 'active',
    notes        = 'Zona norte — Benimàmet y Paterna',
    updated_at   = NOW()
WHERE account_id = 2 AND date_assigned = '2026-05-02'
LIMIT 1;

-- Borrar stops anteriores de hoy y recrear
DELETE FROM `stops`
WHERE account_id = 2
  AND route_id = (SELECT id FROM routes WHERE account_id=2 AND date_assigned='2026-05-02' LIMIT 1);

-- Obtener route_id de hoy para los inserts
SET @hoy_route_id = (SELECT id FROM routes WHERE account_id=2 AND date_assigned='2026-05-02' LIMIT 1);
SET @hoy_route_uid = (SELECT uid FROM routes WHERE account_id=2 AND date_assigned='2026-05-02' LIMIT 1);

INSERT INTO `stops`
(route_id, uid, account_id, name, address,
 lat, lng, order_index,
 external_id, contact_name, contact_phone,
 visit_frequency, priority, segment, account_status, opening_hours,
 status, notes, visited_at, visit_result, next_action,
 created_at, updated_at)
VALUES

-- 0: Farmacia Roig — Carrer de Benimàmet 14
-- 09:30 llegada, 09:48 salida (18 min). 5km desde Ayuntamiento
(@hoy_route_id, 'cccc0002-0000-0000-0000-000000000001', 2,
 'Farmacia Roig', 'Carrer de Benimàmet 14, Valencia',
 39.4904, -0.4183, 0,
 'FARM-00112', 'Marta Roig', '+34 963 157 800',
 30, 1, 'A', 'active',
 '{"mon":"09:00-14:00,17:00-21:00","tue":"09:00-14:00,17:00-21:00","wed":"09:00-14:00,17:00-21:00","thu":"09:00-14:00,17:00-21:00","fri":"09:00-14:00,17:00-21:00","sat":"09:30-14:00"}',
 'done', 'Renovación OK. Muy contenta con el servicio.',
 '2026-05-02 09:30:00', 'contactado',
 'Ofrecer seguro móvil en próxima visita',
 NOW(), '2026-05-02 09:48:00'),

-- 1: Taller Mecánico Vidal — Av. Constitución 88, Paterna
-- 10:15 llegada, 10:50 salida (35 min). 3km desde Farmacia
(@hoy_route_id, 'cccc0002-0000-0000-0000-000000000002', 2,
 'Taller Mecánico Vidal', 'Av. Constitución 88, Paterna',
 39.5005, -0.4401, 1,
 'TALL-00088', 'Jordi Vidal', '+34 961 522 113',
 60, 2, 'B', 'active',
 '{"mon":"08:00-18:00","tue":"08:00-18:00","wed":"08:00-18:00","thu":"08:00-18:00","fri":"08:00-14:00"}',
 'done', 'Quiere cambiar de 4G a fibra — tiene nave con wifi.',
 '2026-05-02 10:15:00', 'contactado',
 'Enviar presupuesto fibra para nave industrial 500Mb',
 NOW(), '2026-05-02 10:50:00'),

-- 2: Óptica Visión Paterna — Carrer Major 3
-- 11:10 llegada, 11:35 salida (25 min). 0.5km desde Taller
(@hoy_route_id, 'cccc0002-0000-0000-0000-000000000003', 2,
 'Óptica Visión Paterna', 'Carrer Major 3, Paterna',
 39.5031, -0.4413, 2,
 'OPTI-00044', 'Ana Llopis', '+34 961 374 892',
 30, 2, 'B', 'active',
 '{"mon":"09:30-13:30,16:30-20:00","tue":"09:30-13:30,16:30-20:00","wed":"09:30-13:30","thu":"09:30-13:30,16:30-20:00","fri":"09:30-13:30,16:30-20:00","sat":"10:00-14:00"}',
 'done', 'Nuevo contrato línea empresa + tablet.',
 '2026-05-02 11:10:00', 'contactado',
 'Gestionar portabilidad del número antiguo',
 NOW(), '2026-05-02 11:35:00'),

-- 3: Gestoría Herrero — Calle Historiador Chabàs 7, Paterna
-- 12:00 llegada, 12:08 salida (8 min). No estaba.
(@hoy_route_id, 'cccc0002-0000-0000-0000-000000000005', 2,
 'Gestoría Herrero & Asociados', 'Calle Historiador Chabàs 7, Paterna',
 39.4978, -0.4432, 3,
 'GEST-00023', 'Isabel Herrero', '+34 961 377 201',
 30, 2, 'B', 'active',
 '{"mon":"09:00-14:00","tue":"09:00-14:00,16:00-18:00","wed":"09:00-14:00","thu":"09:00-14:00,16:00-18:00","fri":"09:00-14:00"}',
 'done', 'Puerta cerrada, estores bajados.',
 '2026-05-02 12:00:00', 'no_estaba',
 'Llamar antes de la próxima visita — preguntar horario actual',
 NOW(), '2026-05-02 12:08:00'),

-- 4: Supermercado El Huerto — Av. Ademuz 201, Paterna
-- PENDIENTE — esta es la que hace el usuario para test
(@hoy_route_id, 'cccc0002-0000-0000-0000-000000000004', 2,
 'Supermercado El Huerto', 'Av. Ademuz 201, Paterna',
 39.5068, -0.4387, 4,
 'SUPE-00067', 'Francisco Moll', '+34 961 480 050',
 30, 3, 'C', 'active',
 '{"mon":"08:00-21:00","tue":"08:00-21:00","wed":"08:00-21:00","thu":"08:00-21:00","fri":"08:00-21:00","sat":"08:00-21:00","sun":"09:00-15:00"}',
 'pending', NULL, NULL, NULL, NULL,
 NOW(), NOW());

-- KPI_VALUES de hoy (solo los 4 ya visitados)
INSERT INTO `kpi_values` (account_id, stop_uid, kpi_id, value_text) VALUES

-- Hoy 0: Farmacia Roig
(2,'cccc0002-0000-0000-0000-000000000001','common_resultado',    'contactado'),
(2,'cccc0002-0000-0000-0000-000000000001','common_duracion',     '18'),
(2,'cccc0002-0000-0000-0000-000000000001','telco_activaciones',  '1'),
(2,'cccc0002-0000-0000-0000-000000000001','telco_bono_eur',      '55.00'),
(2,'cccc0002-0000-0000-0000-000000000001','telco_renovaciones',  '1'),
(2,'cccc0002-0000-0000-0000-000000000001','telco_portabilidades','0'),
(2,'cccc0002-0000-0000-0000-000000000001','telco_churns',        '0'),
(2,'cccc0002-0000-0000-0000-000000000001','telco_stock_sims',    '8'),
(2,'cccc0002-0000-0000-0000-000000000001','telco_plus',          'false'),
(2,'cccc0002-0000-0000-0000-000000000001','common_notas',        'Renovación OK. Muy contenta con el servicio.'),
(2,'cccc0002-0000-0000-0000-000000000001','common_next_action',  'Ofrecer seguro móvil en próxima visita'),

-- Hoy 1: Taller Mecánico Vidal
(2,'cccc0002-0000-0000-0000-000000000002','common_resultado',    'contactado'),
(2,'cccc0002-0000-0000-0000-000000000002','common_duracion',     '35'),
(2,'cccc0002-0000-0000-0000-000000000002','telco_activaciones',  '2'),
(2,'cccc0002-0000-0000-0000-000000000002','telco_bono_eur',      '140.00'),
(2,'cccc0002-0000-0000-0000-000000000002','telco_renovaciones',  '0'),
(2,'cccc0002-0000-0000-0000-000000000002','telco_portabilidades','0'),
(2,'cccc0002-0000-0000-0000-000000000002','telco_churns',        '0'),
(2,'cccc0002-0000-0000-0000-000000000002','telco_stock_sims',    '12'),
(2,'cccc0002-0000-0000-0000-000000000002','telco_plus',          'false'),
(2,'cccc0002-0000-0000-0000-000000000002','common_notas',        'Quiere cambiar de 4G a fibra — tiene nave con wifi.'),
(2,'cccc0002-0000-0000-0000-000000000002','common_next_action',  'Enviar presupuesto fibra para nave industrial 500Mb'),

-- Hoy 2: Óptica Visión
(2,'cccc0002-0000-0000-0000-000000000003','common_resultado',    'contactado'),
(2,'cccc0002-0000-0000-0000-000000000003','common_duracion',     '25'),
(2,'cccc0002-0000-0000-0000-000000000003','telco_activaciones',  '1'),
(2,'cccc0002-0000-0000-0000-000000000003','telco_bono_eur',      '89.00'),
(2,'cccc0002-0000-0000-0000-000000000003','telco_renovaciones',  '1'),
(2,'cccc0002-0000-0000-0000-000000000003','telco_portabilidades','1'),
(2,'cccc0002-0000-0000-0000-000000000003','telco_churns',        '0'),
(2,'cccc0002-0000-0000-0000-000000000003','telco_stock_sims',    '6'),
(2,'cccc0002-0000-0000-0000-000000000003','telco_plus',          'false'),
(2,'cccc0002-0000-0000-0000-000000000003','common_notas',        'Nuevo contrato línea empresa + tablet.'),
(2,'cccc0002-0000-0000-0000-000000000003','common_next_action',  'Gestionar portabilidad del número antiguo'),

-- Hoy 3: Gestoría Herrero (no_estaba)
(2,'cccc0002-0000-0000-0000-000000000005','common_resultado',    'no_estaba'),
(2,'cccc0002-0000-0000-0000-000000000005','common_duracion',     '8'),
(2,'cccc0002-0000-0000-0000-000000000005','common_notas',        'Puerta cerrada, estores bajados.'),
(2,'cccc0002-0000-0000-0000-000000000005','common_next_action',  'Llamar antes de la próxima visita');

-- DAY_SESSION HOY — running (jornada en curso desde las 09:00)
-- started_at: 2026-05-02 07:00:00 UTC = 1746172800000
INSERT INTO `day_sessions`
    (account_id, user_id, route_uid, date_str, state,
     started_at, elapsed_ms, distance_km,
     last_lat, last_lng, updated_at)
VALUES
(2, 2, @hoy_route_uid, '2026-05-02',
 'running',
 1746172800000,  -- 09:00 Valencia
 11280000,       -- 3h8m transcurridos (hasta última parada)
 9.200,
 39.4978, -0.4432,  -- última posición: Gestoría Herrero
 1746183680000);

-- ============================================================
-- VERIFICACIÓN FINAL
-- ============================================================

SELECT
  r.date_assigned AS fecha,
  r.name          AS ruta,
  r.status        AS estado_ruta,
  COUNT(s.id)     AS total_stops,
  SUM(s.status='done')    AS hechas,
  SUM(s.status='pending') AS pendientes
FROM routes r
LEFT JOIN stops s ON s.route_id = r.id AND s.deleted_at IS NULL
WHERE r.account_id = 2 AND r.deleted_at IS NULL
GROUP BY r.id
ORDER BY r.date_assigned;

SELECT date_str, state, elapsed_ms/3600000 AS horas, distance_km
FROM day_sessions WHERE account_id=2;

SELECT COUNT(*) AS total_kpi_values FROM kpi_values WHERE account_id=2;
