-- ============================================================
-- seed_v6_testdata.sql
-- Aplicar DESPUÉS de migration_v6.sql
-- Datos realistas de 3 días: ayer (01-may), hoy (02-may), mañana (03-may)
-- Sector: telco — agente God Admin (user_id=2, account_id=2)
-- ============================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================================
-- 1. FIXES EN DATOS EXISTENTES
-- ============================================================

-- Fix ruta 1: status inconsistente (tiene stops done → marcar active→done)
UPDATE `routes` SET status='done', updated_at='2026-05-01 22:00:00'
WHERE id=1 AND uid='550e8400-e29b-41d4-a716-446655440001';

-- Fix ruta 3: nombre de prueba → ruta real para HOY
UPDATE `routes`
SET name='Ruta Extrarradio Valencia', status='active',
    notes='Zona norte — Benimàmet y Paterna', updated_at='2026-05-02 08:00:00'
WHERE id=3 AND uid='4967c041-589b-4723-b00c-bbc5472dce4d';

-- Fix stops existentes: rellenar todos los campos vacíos
-- Stop 1 — Distribuciones Martínez (done)
UPDATE `stops` SET
    external_id   = 'DIST-00141',
    contact_name  = 'Ramón Martínez',
    contact_phone = '+34 963 410 077',
    visit_result  = 'contactado',
    next_action   = 'Enviar propuesta fibra empresa 300Mb — precio acordado 89€/mes',
    visit_frequency = 30,
    priority      = 2,
    segment       = 'A',
    account_status = 'active',
    opening_hours = '{"mon":"09:00-14:00,16:00-19:00","tue":"09:00-14:00,16:00-19:00","wed":"09:00-14:00,16:00-19:00","thu":"09:00-14:00,16:00-19:00","fri":"09:00-14:00"}'
WHERE id=1;

-- Stop 2 — Electrónica Pérez (done)
UPDATE `stops` SET
    external_id   = 'ELEC-00287',
    contact_name  = 'Sonia Pérez',
    contact_phone = '+34 963 528 901',
    visit_result  = 'contactado',
    next_action   = 'Firmar contrato fibra + 2 móviles el próximo jueves',
    visit_frequency = 14,
    priority      = 1,
    segment       = 'A',
    account_status = 'active',
    opening_hours = '{"mon":"10:00-14:00,17:00-20:00","tue":"10:00-14:00,17:00-20:00","wed":"10:00-14:00,17:00-20:00","thu":"10:00-14:00,17:00-20:00","fri":"10:00-14:00,17:00-20:00","sat":"10:00-14:00"}'
WHERE id=2;

-- Stop 3 — Bar Restaurante El Pilar (pending)
UPDATE `stops` SET
    external_id   = 'BAR-00053',
    contact_name  = 'Miguel Andreu',
    contact_phone = '+34 963 918 332',
    visit_frequency = 60,
    priority      = 3,
    segment       = 'B',
    account_status = 'active',
    opening_hours = '{"mon":"08:00-16:00","tue":"08:00-16:00","wed":"08:00-16:00","thu":"08:00-16:00","fri":"08:00-23:00","sat":"09:00-23:00","sun":"09:00-16:00"}'
WHERE id=3;

-- Stop 4 — Talleres García (done)
UPDATE `stops` SET
    external_id   = 'TALL-00019',
    contact_name  = 'Paco García',
    contact_phone = '+34 961 734 512',
    visit_result  = 'contactado',
    next_action   = 'Revisar factura anterior — posible error cobro doble',
    visit_frequency = 30,
    priority      = 2,
    segment       = 'B',
    account_status = 'active',
    opening_hours = '{"mon":"08:00-18:00","tue":"08:00-18:00","wed":"08:00-18:00","thu":"08:00-18:00","fri":"08:00-14:00"}'
WHERE id=4;

-- Stop 5 — Clínica Dental Noguera (done)
UPDATE `stops` SET
    external_id   = 'CLIN-00076',
    contact_name  = 'Dra. Laura Noguera',
    contact_phone = '+34 963 841 200',
    visit_result  = 'contactado',
    next_action   = 'Alta nueva línea confirmada — enviar SIM por correo',
    visit_frequency = 30,
    priority      = 1,
    segment       = 'A',
    account_status = 'active',
    opening_hours = '{"mon":"09:00-14:00,16:00-20:00","tue":"09:00-14:00,16:00-20:00","wed":"09:00-14:00","thu":"09:00-14:00,16:00-20:00","fri":"09:00-14:00"}'
WHERE id=5;

-- Stop 6 — Academia de Idiomas Oxford (done)
UPDATE `stops` SET
    external_id   = 'ACAD-00031',
    contact_name  = 'Carlos Ribera',
    contact_phone = '+34 963 672 445',
    visit_result  = 'contactado',
    next_action   = 'Ampliar a 5 líneas en septiembre — confirmar en julio',
    visit_frequency = 60,
    priority      = 1,
    segment       = 'A',
    account_status = 'active',
    opening_hours = '{"mon":"09:00-21:00","tue":"09:00-21:00","wed":"09:00-21:00","thu":"09:00-21:00","fri":"09:00-14:00","sat":"09:00-14:00"}'
WHERE id=6;

-- Stop 11 — Qli (done)
UPDATE `stops` SET
    external_id   = 'QLI-00001',
    contact_name  = 'Pablo Salvador',
    contact_phone = '+34 600 000 001',
    visit_result  = 'contactado',
    notes         = 'Stop de prueba creado desde la app',
    next_action   = 'Seguimiento en 2 semanas',
    visit_frequency = 30,
    priority      = 3,
    segment       = 'C',
    account_status = 'active'
WHERE id=11;

-- ============================================================
-- 2. RUTAS + STOPS PARA HOY (2026-05-02) — reemplazar ruta 'lol'
-- ============================================================

-- Stops de Ruta Extrarradio (route_id=3, ya actualizada arriba)
INSERT INTO `stops`
    (route_id, uid, account_id, name, address, lat, lng, order_index,
     external_id, contact_name, contact_phone,
     visit_frequency, priority, segment, account_status, opening_hours,
     status, notes, visited_at, visit_result, next_action,
     created_at, updated_at)
VALUES
-- Stop A — Farmacia Roig (done)
(3, 'cccc0002-0000-0000-0000-000000000001', 2,
 'Farmacia Roig', 'Carrer de Benimàmet 14, Valencia',
 39.4904, -0.4183, 0,
 'FARM-00112', 'Marta Roig', '+34 963 157 800',
 30, 1, 'A', 'active',
 '{"mon":"09:00-14:00,17:00-21:00","tue":"09:00-14:00,17:00-21:00","wed":"09:00-14:00,17:00-21:00","thu":"09:00-14:00,17:00-21:00","fri":"09:00-14:00,17:00-21:00","sat":"09:30-14:00"}',
 'done', 'Renovación OK. Muy contenta con el servicio.',
 '2026-05-02 09:45:00', 'contactado',
 'Ofrecer seguro móvil en próxima visita',
 '2026-05-02 08:00:00', '2026-05-02 09:50:00'),

-- Stop B — Taller Mecánico Vidal (done)
(3, 'cccc0002-0000-0000-0000-000000000002', 2,
 'Taller Mecánico Vidal', 'Av. Constitución 88, Paterna',
 39.5005, -0.4401, 1,
 'TALL-00088', 'Jordi Vidal', '+34 961 522 113',
 60, 2, 'B', 'active',
 '{"mon":"08:00-18:00","tue":"08:00-18:00","wed":"08:00-18:00","thu":"08:00-18:00","fri":"08:00-14:00"}',
 'done', 'Quiere cambiar de 4G a fibra — tiene nave con wifi.',
 '2026-05-02 11:00:00', 'contactado',
 'Enviar presupuesto fibra para nave industrial 500Mb',
 '2026-05-02 08:00:00', '2026-05-02 11:15:00'),

-- Stop C — Óptica Visión (visiting → simular en curso ahora)
(3, 'cccc0002-0000-0000-0000-000000000003', 2,
 'Óptica Visión Paterna', 'Carrer Major 3, Paterna',
 39.5031, -0.4413, 2,
 'OPTI-00044', 'Ana Llopis', '+34 961 374 892',
 30, 2, 'B', 'active',
 '{"mon":"09:30-13:30,16:30-20:00","tue":"09:30-13:30,16:30-20:00","wed":"09:30-13:30","thu":"09:30-13:30,16:30-20:00","fri":"09:30-13:30,16:30-20:00","sat":"10:00-14:00"}',
 'pending', NULL, NULL, NULL, NULL,
 '2026-05-02 08:00:00', '2026-05-02 08:00:00'),

-- Stop D — Supermercado El Huerto (pending)
(3, 'cccc0002-0000-0000-0000-000000000004', 2,
 'Supermercado El Huerto', 'Av. Ademuz 201, Paterna',
 39.5068, -0.4387, 3,
 'SUPE-00067', 'Francisco Moll', '+34 961 480 050',
 30, 3, 'C', 'active',
 '{"mon":"08:00-21:00","tue":"08:00-21:00","wed":"08:00-21:00","thu":"08:00-21:00","fri":"08:00-21:00","sat":"08:00-21:00","sun":"09:00-15:00"}',
 'pending', NULL, NULL, NULL, NULL,
 '2026-05-02 08:00:00', '2026-05-02 08:00:00'),

-- Stop E — Gestoría Herrero (no_estaba)
(3, 'cccc0002-0000-0000-0000-000000000005', 2,
 'Gestoría Herrero & Asociados', 'Calle Historiador Chabàs 7, Paterna',
 39.4978, -0.4432, 4,
 'GEST-00023', 'Isabel Herrero', '+34 961 377 201',
 30, 2, 'B', 'active',
 '{"mon":"09:00-14:00","tue":"09:00-14:00,16:00-18:00","wed":"09:00-14:00","thu":"09:00-14:00,16:00-18:00","fri":"09:00-14:00"}',
 'done', 'Puerta cerrada, estores bajados.',
 '2026-05-02 12:30:00', 'no_estaba',
 'Llamar antes de la próxima visita — preguntar horario actual',
 '2026-05-02 08:00:00', '2026-05-02 12:35:00');

-- ============================================================
-- 3. RUTA + STOPS PARA MAÑANA (2026-05-03)
-- ============================================================

INSERT INTO `routes`
    (account_id, user_id, uid, name, date_assigned, status, notes, created_at, updated_at)
VALUES
(2, 2, 'dddd0003-0000-0000-0000-000000000001',
 'Ruta Zona Puerto', '2026-05-03', 'pending',
 'Visitas zona marítima y Cabanyal — clientes premium',
 '2026-05-02 08:00:00', '2026-05-02 08:00:00');

-- Stops de Ruta Zona Puerto (mañana — todos pending)
INSERT INTO `stops`
    (route_id, uid, account_id, name, address, lat, lng, order_index,
     external_id, contact_name, contact_phone,
     visit_frequency, priority, segment, account_status, opening_hours,
     status, notes, created_at, updated_at)
VALUES
-- Stop 1 — Restaurante La Lonja del Pescador
((SELECT id FROM routes WHERE uid='dddd0003-0000-0000-0000-000000000001'),
 'dddd0003-0000-0000-0000-000000000001', 2,
 'Restaurante La Lonja del Pescador', 'Carrer de la Reina 45, Valencia',
 39.4603, -0.3276, 0,
 'REST-00201', 'Tomàs Ferri', '+34 963 212 030',
 30, 1, 'A', 'active',
 '{"tue":"13:00-16:00,20:00-23:00","wed":"13:00-16:00,20:00-23:00","thu":"13:00-16:00,20:00-23:00","fri":"13:00-16:00,20:00-23:00","sat":"13:00-16:00,20:00-23:00","sun":"13:00-16:30"}',
 'pending', 'Interesado en TPV con conectividad 5G — pendiente demo',
 '2026-05-02 08:00:00', '2026-05-02 08:00:00'),

-- Stop 2 — Hotel Cabanyal Boutique
((SELECT id FROM routes WHERE uid='dddd0003-0000-0000-0000-000000000001'),
 'dddd0003-0000-0000-0000-000000000002', 2,
 'Hotel Cabanyal Boutique', 'Carrer del Rosari 12, Valencia',
 39.4680, -0.3301, 1,
 'HOTE-00038', 'Elena Sánchez', '+34 963 547 820',
 30, 1, 'A', 'active',
 '{"mon":"08:00-20:00","tue":"08:00-20:00","wed":"08:00-20:00","thu":"08:00-20:00","fri":"08:00-20:00","sat":"08:00-20:00","sun":"08:00-20:00"}',
 'pending', '15 habitaciones — necesita wifi roaming y fibra simétrica 1Gb',
 '2026-05-02 08:00:00', '2026-05-02 08:00:00'),

-- Stop 3 — Estudio Fotografía Marina
((SELECT id FROM routes WHERE uid='dddd0003-0000-0000-0000-000000000001'),
 'dddd0003-0000-0000-0000-000000000003', 2,
 'Estudio Fotografía Marina', 'Passeig Neptú 78, Valencia',
 39.4571, -0.3388, 2,
 'FOTO-00015', 'Mireia Costa', '+34 628 441 003',
 60, 3, 'B', 'active',
 '{"mon":"10:00-19:00","tue":"10:00-19:00","wed":"10:00-19:00","thu":"10:00-19:00","fri":"10:00-19:00","sat":"10:00-14:00"}',
 'pending', 'Autónoma — 1 línea móvil + fibra 100Mb. Valorar pack.',
 '2026-05-02 08:00:00', '2026-05-02 08:00:00'),

-- Stop 4 — Clínica Fisioterapia Neptuno
((SELECT id FROM routes WHERE uid='dddd0003-0000-0000-0000-000000000001'),
 'dddd0003-0000-0000-0000-000000000004', 2,
 'Clínica Fisioterapia Neptuno', 'Carrer de Lepant 9, Valencia',
 39.4594, -0.3312, 3,
 'FISI-00092', 'David Ruiz', '+34 963 331 779',
 30, 2, 'B', 'active',
 '{"mon":"09:00-21:00","tue":"09:00-21:00","wed":"09:00-21:00","thu":"09:00-21:00","fri":"09:00-20:00","sat":"09:00-14:00"}',
 'pending', 'Renovación en julio. Visitar antes para asegurar continuidad.',
 '2026-05-02 08:00:00', '2026-05-02 08:00:00'),

-- Stop 5 — Academia Danza Flamenco
((SELECT id FROM routes WHERE uid='dddd0003-0000-0000-0000-000000000001'),
 'dddd0003-0000-0000-0000-000000000005', 2,
 'Academia Danza Flamenco Cabanyal', 'Carrer de la Barraca 33, Valencia',
 39.4711, -0.3287, 4,
 'ACAD-00088', 'Rosa Molina', '+34 963 482 617',
 90, 4, 'C', 'active',
 '{"mon":"16:00-21:00","tue":"16:00-21:00","wed":"16:00-21:00","thu":"16:00-21:00","fri":"16:00-21:00","sat":"10:00-14:00"}',
 'pending', 'Cliente base — no crecer mucho. Revisión anual.',
 '2026-05-02 08:00:00', '2026-05-02 08:00:00');

-- ============================================================
-- 4. DAY_SESSIONS (requiere migration_v6 aplicada)
-- ============================================================

INSERT INTO `day_sessions`
    (account_id, user_id, route_uid, date_str, state,
     started_at, elapsed_ms, distance_km,
     last_lat, last_lng, updated_at)
VALUES
-- Ayer: Ruta Centro — jornada completada
(2, 2, '550e8400-e29b-41d4-a716-446655440001', '2026-05-01',
 'done', 1746093600000, 18540000, 12.400,
 39.4753, -0.3749, 1746112140000),

-- Ayer: Ruta Norte — jornada completada
(2, 2, '550e8400-e29b-41d4-a716-446655440002', '2026-05-01',
 'done', 1746090000000, 21600000, 15.200,
 39.4833, -0.3756, 1746111600000),

-- Hoy: Ruta Extrarradio — en curso (running)
(2, 2, '4967c041-589b-4723-b00c-bbc5472dce4d', '2026-05-02',
 'running', 1746176400000, 7200000, 8.750,
 39.5031, -0.4413, 1746183600000);

-- ============================================================
-- 5. KPI_VALUES (requiere migration_v6 aplicada)
-- Valores telco para los stops completados de ayer
-- ============================================================

INSERT INTO `kpi_values` (account_id, stop_uid, kpi_id, value_text) VALUES
-- Stop 1 Distribuciones Martínez
(2, 'aaaa0001-0000-0000-0000-000000000001', 'common_resultado',   'contactado'),
(2, 'aaaa0001-0000-0000-0000-000000000001', 'common_duracion',    '25'),
(2, 'aaaa0001-0000-0000-0000-000000000001', 'telco_activaciones', '2'),
(2, 'aaaa0001-0000-0000-0000-000000000001', 'telco_bono_eur',     '178.00'),
(2, 'aaaa0001-0000-0000-0000-000000000001', 'telco_renovaciones', '1'),
(2, 'aaaa0001-0000-0000-0000-000000000001', 'telco_plus',         'false'),
(2, 'aaaa0001-0000-0000-0000-000000000001', 'common_notas',       'Pedir albarán firmado'),
(2, 'aaaa0001-0000-0000-0000-000000000001', 'common_next_action', 'Enviar propuesta fibra empresa 300Mb'),

-- Stop 2 Electrónica Pérez
(2, 'aaaa0001-0000-0000-0000-000000000002', 'common_resultado',   'contactado'),
(2, 'aaaa0001-0000-0000-0000-000000000002', 'common_duracion',    '40'),
(2, 'aaaa0001-0000-0000-0000-000000000002', 'telco_activaciones', '3'),
(2, 'aaaa0001-0000-0000-0000-000000000002', 'telco_bono_eur',     '267.00'),
(2, 'aaaa0001-0000-0000-0000-000000000002', 'telco_renovaciones', '2'),
(2, 'aaaa0001-0000-0000-0000-000000000002', 'telco_portabilidades','1'),
(2, 'aaaa0001-0000-0000-0000-000000000002', 'telco_plus',         'true'),
(2, 'aaaa0001-0000-0000-0000-000000000002', 'common_notas',       'Renovar contrato fibra'),
(2, 'aaaa0001-0000-0000-0000-000000000002', 'common_next_action', 'Firmar contrato fibra + 2 móviles el próximo jueves'),

-- Stop 4 Talleres García
(2, 'aaaa0001-0000-0000-0000-000000000004', 'common_resultado',   'contactado'),
(2, 'aaaa0001-0000-0000-0000-000000000004', 'common_duracion',    '30'),
(2, 'aaaa0001-0000-0000-0000-000000000004', 'telco_activaciones', '1'),
(2, 'aaaa0001-0000-0000-0000-000000000004', 'telco_bono_eur',     '89.00'),
(2, 'aaaa0001-0000-0000-0000-000000000004', 'telco_renovaciones', '0'),
(2, 'aaaa0001-0000-0000-0000-000000000004', 'telco_plus',         'false'),
(2, 'aaaa0001-0000-0000-0000-000000000004', 'common_notas',       'Visita técnica instalación'),
(2, 'aaaa0001-0000-0000-0000-000000000004', 'common_next_action', 'Revisar factura anterior'),

-- Stop 5 Clínica Dental Noguera
(2, 'aaaa0001-0000-0000-0000-000000000005', 'common_resultado',   'contactado'),
(2, 'aaaa0001-0000-0000-0000-000000000005', 'common_duracion',    '20'),
(2, 'aaaa0001-0000-0000-0000-000000000005', 'telco_activaciones', '1'),
(2, 'aaaa0001-0000-0000-0000-000000000005', 'telco_bono_eur',     '45.00'),
(2, 'aaaa0001-0000-0000-0000-000000000005', 'telco_renovaciones', '1'),
(2, 'aaaa0001-0000-0000-0000-000000000005', 'telco_plus',         'false'),
(2, 'aaaa0001-0000-0000-0000-000000000005', 'common_notas',       'Alta nueva línea móvil'),
(2, 'aaaa0001-0000-0000-0000-000000000005', 'common_next_action', 'Alta nueva línea confirmada — enviar SIM'),

-- Stop 6 Academia Oxford
(2, 'aaaa0001-0000-0000-0000-000000000006', 'common_resultado',   'contactado'),
(2, 'aaaa0001-0000-0000-0000-000000000006', 'common_duracion',    '45'),
(2, 'aaaa0001-0000-0000-0000-000000000006', 'telco_activaciones', '3'),
(2, 'aaaa0001-0000-0000-0000-000000000006', 'telco_bono_eur',     '267.00'),
(2, 'aaaa0001-0000-0000-0000-000000000006', 'telco_renovaciones', '3'),
(2, 'aaaa0001-0000-0000-0000-000000000006', 'telco_portabilidades','2'),
(2, 'aaaa0001-0000-0000-0000-000000000006', 'telco_plus',         'true'),
(2, 'aaaa0001-0000-0000-0000-000000000006', 'common_notas',       'Contrato firmado — 3 líneas'),
(2, 'aaaa0001-0000-0000-0000-000000000006', 'common_next_action', 'Ampliar a 5 líneas en septiembre'),

-- Hoy: Stop A Farmacia Roig (ya visitada)
(2, 'cccc0002-0000-0000-0000-000000000001', 'common_resultado',   'contactado'),
(2, 'cccc0002-0000-0000-0000-000000000001', 'common_duracion',    '18'),
(2, 'cccc0002-0000-0000-0000-000000000001', 'telco_activaciones', '1'),
(2, 'cccc0002-0000-0000-0000-000000000001', 'telco_bono_eur',     '55.00'),
(2, 'cccc0002-0000-0000-0000-000000000001', 'telco_renovaciones', '1'),
(2, 'cccc0002-0000-0000-0000-000000000001', 'telco_plus',         'false'),
(2, 'cccc0002-0000-0000-0000-000000000001', 'common_notas',       'Renovación OK. Muy contenta con el servicio.'),
(2, 'cccc0002-0000-0000-0000-000000000001', 'common_next_action', 'Ofrecer seguro móvil en próxima visita'),

-- Hoy: Stop B Taller Mecánico Vidal
(2, 'cccc0002-0000-0000-0000-000000000002', 'common_resultado',   'contactado'),
(2, 'cccc0002-0000-0000-0000-000000000002', 'common_duracion',    '35'),
(2, 'cccc0002-0000-0000-0000-000000000002', 'telco_activaciones', '2'),
(2, 'cccc0002-0000-0000-0000-000000000002', 'telco_bono_eur',     '140.00'),
(2, 'cccc0002-0000-0000-0000-000000000002', 'telco_renovaciones', '0'),
(2, 'cccc0002-0000-0000-0000-000000000002', 'telco_plus',         'false'),
(2, 'cccc0002-0000-0000-0000-000000000002', 'common_notas',       'Quiere cambiar de 4G a fibra — tiene nave con wifi.'),
(2, 'cccc0002-0000-0000-0000-000000000002', 'common_next_action', 'Enviar presupuesto fibra para nave industrial 500Mb'),

-- Hoy: Stop E Gestoría Herrero (no_estaba)
(2, 'cccc0002-0000-0000-0000-000000000005', 'common_resultado',   'no_estaba'),
(2, 'cccc0002-0000-0000-0000-000000000005', 'common_duracion',    '5'),
(2, 'cccc0002-0000-0000-0000-000000000005', 'common_notas',       'Puerta cerrada, estores bajados.'),
(2, 'cccc0002-0000-0000-0000-000000000005', 'common_next_action', 'Llamar antes de la próxima visita');

-- ============================================================
-- 6. BUSINESS_PROFILE (sector telco para God Admin)
-- ============================================================

INSERT INTO `business_profiles` (account_id, sector, name, updated_at)
VALUES (2, 'telco', 'God Admin', UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE sector='telco', name='God Admin', updated_at=UNIX_TIMESTAMP() * 1000;

-- ============================================================
-- Verificación rápida
-- ============================================================

SELECT 'routes' AS tabla, COUNT(*) AS total, MIN(date_assigned) AS desde, MAX(date_assigned) AS hasta FROM routes WHERE deleted_at IS NULL
UNION ALL
SELECT 'stops', COUNT(*), MIN(created_at), MAX(created_at) FROM stops WHERE deleted_at IS NULL
UNION ALL
SELECT 'day_sessions', COUNT(*), MIN(date_str), MAX(date_str) FROM day_sessions
UNION ALL
SELECT 'kpi_values', COUNT(*), NULL, NULL FROM kpi_values
UNION ALL
SELECT 'business_profiles', COUNT(*), NULL, NULL FROM business_profiles;
