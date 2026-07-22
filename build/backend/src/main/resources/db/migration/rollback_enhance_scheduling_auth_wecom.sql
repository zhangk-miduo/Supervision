-- Manual rollback reference. Back up data before use.
DROP TABLE IF EXISTS supervision_task_trigger;
DROP TABLE IF EXISTS supervision_message_delivery;
DROP TABLE IF EXISTS supervision_task_recipient;
DROP TABLE IF EXISTS supervision_message_template;
DROP TABLE IF EXISTS supervision_wecom_webhook;
DROP TABLE IF EXISTS supervision_org_sync_log;
DROP TABLE IF EXISTS supervision_person_department;
ALTER TABLE supervision_account DROP FOREIGN KEY fk_account_person;
DROP TABLE IF EXISTS supervision_person;
DROP TABLE IF EXISTS supervision_department;
DROP TABLE IF EXISTS supervision_wecom_config;