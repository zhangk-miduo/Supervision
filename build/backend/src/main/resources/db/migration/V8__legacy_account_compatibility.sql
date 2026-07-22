SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='supervision_account' AND column_name='failed_login_count')=0,
  'ALTER TABLE supervision_account ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='supervision_account' AND column_name='locked_until')=0,
  'ALTER TABLE supervision_account ADD COLUMN locked_until DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='supervision_account' AND column_name='password_changed_at')=0,
  'ALTER TABLE supervision_account ADD COLUMN password_changed_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='supervision_account' AND column_name='created_by')=0,
  'ALTER TABLE supervision_account ADD COLUMN created_by BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @dml = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='supervision_account' AND column_name='failed_count')=1,
  'UPDATE supervision_account SET failed_login_count=failed_count', 'SELECT 1');
PREPARE stmt FROM @dml; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @dml = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='supervision_account' AND column_name='lock_until')=1,
  'UPDATE supervision_account SET locked_until=lock_until', 'SELECT 1');
PREPARE stmt FROM @dml; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE supervision_account SET status = CASE WHEN CAST(status AS CHAR) IN ('ACTIVE','1') THEN '1' ELSE '0' END;
ALTER TABLE supervision_account MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1;

SET @dml = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='supervision_account' AND column_name='role')=1,
  'INSERT IGNORE INTO supervision_account_role(account_id,role_id) SELECT a.id,r.id FROM supervision_account a JOIN supervision_role r ON r.code=CASE WHEN a.role=''ADMIN'' THEN ''ADMIN'' ELSE ''OPERATOR'' END',
  'SELECT 1');
PREPARE stmt FROM @dml; EXECUTE stmt; DEALLOCATE PREPARE stmt;