-- Upgrade organization tables created by legacy releases without dropping data or changing primary keys.
-- Missing external identifiers are deterministically derived from the preserved local primary key.

DROP PROCEDURE IF EXISTS supervision_add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE supervision_add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_definition_value VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN `', column_name_value, '` ', column_definition_value);
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$
DELIMITER ;

CALL supervision_add_column_if_missing('supervision_department', 'tenant_key', 'VARCHAR(64) NULL');
CALL supervision_add_column_if_missing('supervision_department', 'wecom_department_id', 'BIGINT NULL');
CALL supervision_add_column_if_missing('supervision_department', 'parent_wecom_department_id', 'BIGINT NULL');
CALL supervision_add_column_if_missing('supervision_department', 'name', 'VARCHAR(128) NULL');
CALL supervision_add_column_if_missing('supervision_department', 'order_no', 'INT NOT NULL DEFAULT 0');
CALL supervision_add_column_if_missing('supervision_department', 'status', 'TINYINT NOT NULL DEFAULT 1');
CALL supervision_add_column_if_missing('supervision_department', 'synced_at', 'DATETIME NULL');

CALL supervision_add_column_if_missing('supervision_person', 'tenant_key', 'VARCHAR(64) NULL');
CALL supervision_add_column_if_missing('supervision_person', 'wecom_user_id', 'VARCHAR(128) NULL');
CALL supervision_add_column_if_missing('supervision_person', 'name', 'VARCHAR(128) NULL');
CALL supervision_add_column_if_missing('supervision_person', 'mobile_masked', 'VARCHAR(32) NULL');
CALL supervision_add_column_if_missing('supervision_person', 'email', 'VARCHAR(255) NULL');
CALL supervision_add_column_if_missing('supervision_person', 'position_name', 'VARCHAR(128) NULL');
CALL supervision_add_column_if_missing('supervision_person', 'avatar_url', 'VARCHAR(512) NULL');
CALL supervision_add_column_if_missing('supervision_person', 'status', 'TINYINT NOT NULL DEFAULT 1');
CALL supervision_add_column_if_missing('supervision_person', 'synced_at', 'DATETIME NULL');

CALL supervision_add_column_if_missing('supervision_person_department', 'person_id', 'BIGINT NULL');
CALL supervision_add_column_if_missing('supervision_person_department', 'department_id', 'BIGINT NULL');
CALL supervision_add_column_if_missing('supervision_person_department', 'is_main', 'TINYINT NOT NULL DEFAULT 0');

DROP PROCEDURE supervision_add_column_if_missing;

UPDATE supervision_department
SET tenant_key = 'default'
WHERE tenant_key IS NULL OR tenant_key = '';
UPDATE supervision_department
SET wecom_department_id = id
WHERE wecom_department_id IS NULL;
UPDATE supervision_department
SET name = CONCAT('历史部门-', id)
WHERE name IS NULL OR name = '';

UPDATE supervision_person
SET tenant_key = 'default'
WHERE tenant_key IS NULL OR tenant_key = '';
UPDATE supervision_person
SET wecom_user_id = CONCAT('legacy-', id)
WHERE wecom_user_id IS NULL OR wecom_user_id = '';
UPDATE supervision_person
SET name = CONCAT('历史人员-', id)
WHERE name IS NULL OR name = '';

ALTER TABLE supervision_department
    MODIFY tenant_key VARCHAR(64) NOT NULL,
    MODIFY wecom_department_id BIGINT NOT NULL,
    MODIFY name VARCHAR(128) NOT NULL;
ALTER TABLE supervision_person
    MODIFY tenant_key VARCHAR(64) NOT NULL,
    MODIFY wecom_user_id VARCHAR(128) NOT NULL,
    MODIFY name VARCHAR(128) NOT NULL;

DROP PROCEDURE IF EXISTS supervision_add_index_if_missing;
DELIMITER $$
CREATE PROCEDURE supervision_add_index_if_missing(
    IN table_name_value VARCHAR(64),
    IN index_name_value VARCHAR(64),
    IN index_definition_value VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND index_name = index_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD ', index_definition_value);
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$
DELIMITER ;

CALL supervision_add_index_if_missing('supervision_department', 'uk_department_external', 'UNIQUE KEY `uk_department_external` (`tenant_key`, `wecom_department_id`)');
CALL supervision_add_index_if_missing('supervision_person', 'uk_person_external', 'UNIQUE KEY `uk_person_external` (`tenant_key`, `wecom_user_id`)');
CALL supervision_add_index_if_missing('supervision_person_department', 'uk_person_department', 'UNIQUE KEY `uk_person_department` (`person_id`, `department_id`)');
DROP PROCEDURE supervision_add_index_if_missing;
