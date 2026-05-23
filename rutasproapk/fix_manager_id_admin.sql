-- Asignar manager_id del admin al owner de la cuenta
-- admin (id:7, account:3) reporta a owner (id:3, account:3)
UPDATE users SET manager_id = 3 WHERE id = 7 AND account_id = 3;

-- Verificar resultado
SELECT id, username, role, manager_id FROM users WHERE account_id = 3 ORDER BY id;
