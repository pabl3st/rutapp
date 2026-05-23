-- ══════════════════════════════════════════════════════════════════
-- FIX PRODUCCIÓN — basado en dump cqvkelal_rutasapp_android__11_
-- Ejecutar en phpMyAdmin → pestaña SQL → Ejecutar
-- Todos los INSERT usan ON DUPLICATE KEY UPDATE / INSERT IGNORE
-- Safe para re-ejecutar
-- ══════════════════════════════════════════════════════════════════

SET NAMES utf8mb4;

-- ══════════════════════════════════════════════════════════════════
-- 1. KPI_DEFINITIONS — VACÍA (crítico: la app no muestra formularios)
--    Catálogo completo extraído de KpiCatalog.kt (fuente de verdad)
-- ══════════════════════════════════════════════════════════════════

INSERT IGNORE INTO kpi_definitions
    (id, account_id, sector, label, type, unit, options, required, visible, order_index, section, is_system)
VALUES
-- ── Comunes (todos los sectores) ─────────────────────────────
('common_resultado',    0,'common','Resultado visita','select',NULL,'["contactado","no_estaba","volvemos","rechazado"]',1,1,0,'general',1),
('common_duracion',     0,'common','Duración (min)','number','min',NULL,0,1,1,'general',1),
('common_notas',        0,'common','Notas','text',NULL,NULL,0,1,0,'notas',1),
('common_next_action',  0,'common','Próxima acción','text',NULL,NULL,0,1,1,'notas',1),

-- ── Telco ─────────────────────────────────────────────────────
('telco_activaciones',  0,'telco','Activaciones','number','ud',NULL,1,1,0,'objetivos',1),
('telco_bono_eur',      0,'telco','Importe bono (€)','number','€',NULL,0,1,1,'objetivos',1),
('telco_renovaciones',  0,'telco','Renovaciones','number','ud',NULL,0,1,2,'objetivos',1),
('telco_portabilidades',0,'telco','Portabilidades ent.','number','ud',NULL,0,1,3,'objetivos',1),
('telco_churns',        0,'telco','Churns (bajas)','number','ud',NULL,0,1,4,'objetivos',1),
('telco_plus',          0,'telco','Plus conseguido','boolean',NULL,NULL,0,1,5,'objetivos',1),
('telco_tv',            0,'telco','Televisión (ud)','number','ud',NULL,0,0,6,'objetivos',1),
('telco_stock_sims',    0,'telco','Stock SIMs','number','ud',NULL,0,1,0,'pedidos',1),
('telco_pedido_eur',    0,'telco','Pedido (€)','number','€',NULL,0,1,1,'pedidos',1),
('telco_primer_bono',   0,'telco','Importe 1er bono','number','€',NULL,0,1,0,'resultados',1),
('telco_media_bono',    0,'telco','Media bono (€)','number','€',NULL,0,1,1,'resultados',1),
('telco_recargas',      0,'telco','Recargas','number','ud',NULL,0,1,2,'resultados',1),
('telco_pdv_inactivo',  0,'telco','PDV inactivo','boolean',NULL,NULL,0,1,3,'resultados',1),
('telco_plv',           0,'telco','PLV colocado','boolean',NULL,NULL,0,1,4,'resultados',1),
('telco_stock',         0,'telco','Stock OK','boolean',NULL,NULL,0,0,5,'resultados',1),

-- ── Farmacia ──────────────────────────────────────────────────
('farma_unidades',      0,'farma','Unidades vendidas','number','ud',NULL,1,1,0,'objetivos',1),
('farma_pedido_eur',    0,'farma','Pedido (€)','number','€',NULL,1,1,0,'pedidos',1),
('farma_referencias',   0,'farma','Refs. activas','number','ud',NULL,0,1,1,'objetivos',1),
('farma_facing',        0,'farma','Facing en lineal','number','ud',NULL,0,1,2,'objetivos',1),
('farma_caducidades',   0,'farma','Caducidades retir.','number','ud',NULL,0,1,1,'pedidos',1),
('farma_devoluciones',  0,'farma','Devoluciones','number','ud',NULL,0,0,2,'pedidos',1),
('farma_promo_activa',  0,'farma','Promoción activa','boolean',NULL,NULL,0,1,3,'objetivos',1),

-- ── Distribución ──────────────────────────────────────────────
('dist_pedido_eur',     0,'distribucion','Pedido (€)','number','€',NULL,1,1,0,'pedidos',1),
('dist_referencias',    0,'distribucion','Refs. pedidas','number','ud',NULL,0,1,1,'pedidos',1),
('dist_incidencias',    0,'distribucion','Incidencias logíst.','number','ud',NULL,0,1,0,'general',1),
('dist_exposicion',     0,'distribucion','Exposición OK','boolean',NULL,NULL,0,1,0,'objetivos',1),
('dist_competencia',    0,'distribucion','Acción competencia','text',NULL,NULL,0,0,2,'notas',1),

-- ── Retail ────────────────────────────────────────────────────
('retail_sellout',      0,'retail','Sell-out (ud)','number','ud',NULL,1,1,0,'objetivos',1),
('retail_pedido_eur',   0,'retail','Pedido reposición €','number','€',NULL,0,1,0,'pedidos',1),
('retail_rotacion',     0,'retail','Rotación (días)','number','d',NULL,0,1,1,'objetivos',1),
('retail_promo',        0,'retail','Promociones activas','number','ud',NULL,0,1,2,'objetivos',1),
('retail_oos',          0,'retail','Rotura de stock','boolean',NULL,NULL,0,1,3,'objetivos',1),
('retail_competencia',  0,'retail','Precio competencia €','number','€',NULL,0,0,2,'notas',1);

-- ══════════════════════════════════════════════════════════════════
-- 2. BUSINESS_PROFILES — faltan cuentas 1 (Pablo) y 3 (Empresa Demo)
--    La app no puede guardar perfil de negocio sin este registro
-- ══════════════════════════════════════════════════════════════════

INSERT IGNORE INTO business_profiles (account_id, sector, name, updated_at) VALUES
(1, 'telco', 'Pablo Salvador Poveda', 0),
(3, 'telco', 'Empresa Demo SA',       0);

-- ══════════════════════════════════════════════════════════════════
-- 3. SCHEMA_MIGRATIONS — registrar v12 y v13 (columnas ya existen en stops)
--    Evita que el script de migración vuelva a intentar añadirlas
-- ══════════════════════════════════════════════════════════════════

INSERT IGNORE INTO schema_migrations (version, applied_at) VALUES
('v12.0.0', NOW()),
('v13.0.0', NOW()),
('v14.0.0', NOW());

-- ══════════════════════════════════════════════════════════════════
-- 4. STOPS — columnas de migración v12/v13 (ya en DDL, solo verificar)
--    Si da error "Duplicate column" ignorar — ya está aplicado
-- ══════════════════════════════════════════════════════════════════

-- Estas columnas ya están en el DDL del dump (stops ya las tiene):
-- check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit, date_assigned
-- NO necesitan ALTER TABLE

-- ══════════════════════════════════════════════════════════════════
-- 5. ROUTES — columna scheduled_dates: el comentario dice JSON pero
--    la app envía CSV. Actualizar comentario (no afecta funcionalidad)
-- ══════════════════════════════════════════════════════════════════

ALTER TABLE routes MODIFY COLUMN scheduled_dates TEXT DEFAULT NULL
    COMMENT 'Fechas adicionales en CSV: "2026-05-12,2026-05-21" (además de date_assigned)';

-- ══════════════════════════════════════════════════════════════════
-- 6. USERS — verificar índice manager_id (ya existe en dump, solo asegurar)
-- ══════════════════════════════════════════════════════════════════

-- idx_manager ya está en el dump para users — no necesita ALTER

-- ══════════════════════════════════════════════════════════════════
-- 7. PUSH_TOKENS — añadir fcm_token a sessions si falta
--    sessions.fcm_token ya está en el DDL del dump
-- ══════════════════════════════════════════════════════════════════

-- sessions.fcm_token ya existe en el dump — OK

-- ══════════════════════════════════════════════════════════════════
-- VERIFICACIÓN FINAL
-- ══════════════════════════════════════════════════════════════════

SELECT 'kpi_definitions' AS tabla, COUNT(*) AS filas FROM kpi_definitions
UNION ALL
SELECT 'business_profiles', COUNT(*) FROM business_profiles
UNION ALL
SELECT 'schema_migrations', COUNT(*) FROM schema_migrations
UNION ALL
SELECT 'routes', COUNT(*) FROM routes
UNION ALL
SELECT 'stops', COUNT(*) FROM stops
UNION ALL
SELECT 'kpi_values', COUNT(*) FROM kpi_values
UNION ALL
SELECT 'users', COUNT(*) FROM users;

-- Verificar kpi_definitions por sector
SELECT sector, COUNT(*) AS kpis FROM kpi_definitions GROUP BY sector ORDER BY sector;

-- Verificar jerarquía de usuarios
SELECT u.id, u.username, u.role, u.manager_id,
       m.username AS manager_username, m.role AS manager_role
FROM users u
LEFT JOIN users m ON m.id = u.manager_id
ORDER BY u.account_id, u.id;
