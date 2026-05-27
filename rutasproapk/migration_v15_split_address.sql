-- migration_v15_split_address.sql
-- Separar `address` en componentes: street, postal_code, city.
-- El campo `address` se mantiene para compatibilidad y display.
-- Idempotente: usa IF NOT EXISTS.

ALTER TABLE stops
  ADD COLUMN IF NOT EXISTS street      VARCHAR(300) NULL AFTER address,
  ADD COLUMN IF NOT EXISTS postal_code VARCHAR(10)  NULL AFTER street,
  ADD COLUMN IF NOT EXISTS city        VARCHAR(100) NULL AFTER postal_code;

-- Índice para búsquedas por CP/ciudad (filtros futuros).
CREATE INDEX IF NOT EXISTS idx_stops_postal_code ON stops (postal_code);
CREATE INDEX IF NOT EXISTS idx_stops_city        ON stops (city);

-- Back-fill de datos existentes — extraer CP (5 dígitos) y separar.
-- Regex MariaDB: REGEXP_SUBSTR para capturar el CP de 5 dígitos.
-- Si la dirección no contiene un CP de 5 dígitos, los campos quedan NULL
-- y la calle hereda el address completo.
UPDATE stops
SET
  postal_code = REGEXP_SUBSTR(address, '[0-9]{5}'),
  city = TRIM(
    REGEXP_REPLACE(
      SUBSTRING(address, LOCATE(REGEXP_SUBSTR(address, '[0-9]{5}'), address) + 5),
      '^[ ,]+', ''
    )
  ),
  street = TRIM(
    REGEXP_REPLACE(
      SUBSTRING(address, 1, LOCATE(REGEXP_SUBSTR(address, '[0-9]{5}'), address) - 1),
      '[ ,]+$', ''
    )
  )
WHERE address IS NOT NULL
  AND address REGEXP '[0-9]{5}'
  AND postal_code IS NULL;

-- Para direcciones sin CP detectable, copiar address tal cual en street.
UPDATE stops
SET street = address
WHERE address IS NOT NULL
  AND postal_code IS NULL
  AND street IS NULL;

-- Registrar la migración (idempotente).
INSERT INTO schema_migrations (version, applied_at)
VALUES ('v15_split_address', NOW())
ON DUPLICATE KEY UPDATE applied_at = applied_at;
