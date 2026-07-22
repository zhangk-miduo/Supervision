DELETE ar FROM supervision_account_role ar
JOIN supervision_account a ON a.id=ar.account_id
JOIN supervision_role r ON r.id=ar.role_id
WHERE a.username='admin' AND r.code='OPERATOR';

INSERT IGNORE INTO supervision_account_role(account_id,role_id)
SELECT a.id,r.id FROM supervision_account a
JOIN supervision_role r ON r.code='ADMIN'
WHERE a.username='admin';