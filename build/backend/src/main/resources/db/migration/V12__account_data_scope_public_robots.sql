-- Account-scoped tasks, executions and group webhooks with opt-in public sharing.
DROP PROCEDURE IF EXISTS supervision_v12_add_column;
DELIMITER $$
CREATE PROCEDURE supervision_v12_add_column(IN t VARCHAR(64), IN c VARCHAR(64), IN d VARCHAR(512))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=t AND column_name=c) THEN
        SET @ddl=CONCAT('ALTER TABLE `',t,'` ADD COLUMN `',c,'` ',d);
        PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;

CALL supervision_v12_add_column('supervision_task','owner_account_id','BIGINT NULL');
CALL supervision_v12_add_column('supervision_task_execution','owner_account_id','BIGINT NULL');
CALL supervision_v12_add_column('supervision_task_execution','triggered_by_account_id','BIGINT NULL');
CALL supervision_v12_add_column('supervision_wecom_group','owner_account_id','BIGINT NULL');
CALL supervision_v12_add_column('supervision_wecom_webhook','owner_account_id','BIGINT NULL');
CALL supervision_v12_add_column('supervision_wecom_webhook','is_public','TINYINT NOT NULL DEFAULT 0');
DROP PROCEDURE supervision_v12_add_column;

-- Exact legacy creator matches are safe. Ambiguous rows remain NULL and administrator-only.
UPDATE supervision_task t
JOIN supervision_account a ON a.username=t.created_by
SET t.owner_account_id=a.id
WHERE t.owner_account_id IS NULL AND t.created_by IS NOT NULL AND t.created_by<>'';

UPDATE supervision_task_execution e
JOIN supervision_task t ON t.id=e.task_id
SET e.owner_account_id=t.owner_account_id
WHERE e.owner_account_id IS NULL AND t.owner_account_id IS NOT NULL;

-- Old robots did not record creators. Assign them only when the database has exactly one administrator.
SET @v12_unique_admin=(
    SELECT IF(COUNT(DISTINCT a.id)=1,MIN(a.id),NULL)
    FROM supervision_account a
    JOIN supervision_account_role ar ON ar.account_id=a.id
    JOIN supervision_role r ON r.id=ar.role_id AND r.code='ADMIN'
);
UPDATE supervision_wecom_webhook SET owner_account_id=@v12_unique_admin
WHERE owner_account_id IS NULL AND @v12_unique_admin IS NOT NULL;
UPDATE supervision_wecom_group SET owner_account_id=@v12_unique_admin
WHERE owner_account_id IS NULL AND @v12_unique_admin IS NOT NULL;
UPDATE supervision_wecom_webhook SET is_public=0 WHERE is_public IS NULL;

DROP PROCEDURE IF EXISTS supervision_v12_add_index;
DELIMITER $$
CREATE PROCEDURE supervision_v12_add_index(IN t VARCHAR(64), IN i VARCHAR(64), IN d VARCHAR(512))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=t AND index_name=i) THEN
        SET @ddl=CONCAT('ALTER TABLE `',t,'` ADD ',d);
        PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;
CALL supervision_v12_add_index('supervision_task','idx_task_owner_id','INDEX `idx_task_owner_id` (`owner_account_id`,`id`)');
CALL supervision_v12_add_index('supervision_task_execution','idx_execution_owner_id','INDEX `idx_execution_owner_id` (`owner_account_id`,`id`)');
CALL supervision_v12_add_index('supervision_wecom_webhook','idx_webhook_owner_public','INDEX `idx_webhook_owner_public` (`owner_account_id`,`is_public`,`status`)');
DROP PROCEDURE supervision_v12_add_index;

-- Group names are unique per owner. NULL owners are migration-only and remain administrator-visible.
SET @v12_drop_group_index=IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='supervision_wecom_group' AND index_name='uk_wecom_group_name'),
    'ALTER TABLE supervision_wecom_group DROP INDEX uk_wecom_group_name',
    'SELECT 1'
);
PREPARE st FROM @v12_drop_group_index; EXECUTE st; DEALLOCATE PREPARE st;

SET @v12_add_group_index=IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='supervision_wecom_group' AND index_name='uk_wecom_group_owner_name'),
    'SELECT 1',
    'ALTER TABLE supervision_wecom_group ADD UNIQUE INDEX uk_wecom_group_owner_name(tenant_key,owner_account_id,group_name)'
);
PREPARE st FROM @v12_add_group_index; EXECUTE st; DEALLOCATE PREPARE st;
