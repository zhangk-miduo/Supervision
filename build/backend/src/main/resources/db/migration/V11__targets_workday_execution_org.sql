-- Multi-target WeCom, national workday, execution snapshots and organization observability.
CREATE TABLE IF NOT EXISTS supervision_wecom_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_key VARCHAR(64) NOT NULL DEFAULT 'default',
    group_name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wecom_group_name(tenant_key, group_name)
);

CREATE TABLE IF NOT EXISTS supervision_workday_calendar_year (
    year_value INT PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    source_url VARCHAR(1024) NOT NULL,
    source_document VARCHAR(255) NOT NULL,
    version_value VARCHAR(64) NOT NULL,
    published_at DATE NOT NULL,
    imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checksum_value VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS supervision_workday_calendar_exception (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    year_value INT NOT NULL,
    date_value DATE NOT NULL,
    day_type VARCHAR(16) NOT NULL,
    holiday_name VARCHAR(64) NULL,
    note VARCHAR(255) NULL,
    UNIQUE KEY uk_workday_date(year_value, date_value)
);

DROP PROCEDURE IF EXISTS supervision_v11_add_column;
DELIMITER $$
CREATE PROCEDURE supervision_v11_add_column(IN t VARCHAR(64), IN c VARCHAR(64), IN d VARCHAR(512))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=t AND column_name=c) THEN
        SET @ddl=CONCAT('ALTER TABLE `',t,'` ADD COLUMN `',c,'` ',d);
        PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;

CALL supervision_v11_add_column('supervision_wecom_webhook','group_id','BIGINT NULL');
CALL supervision_v11_add_column('supervision_wecom_webhook','push_name','VARCHAR(128) NULL');
CALL supervision_v11_add_column('supervision_wecom_webhook','system_code','VARCHAR(64) NULL');
CALL supervision_v11_add_column('supervision_wecom_webhook','updated_at','DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

CALL supervision_v11_add_column('supervision_task_execution','task_name_snapshot','VARCHAR(255) NULL');
CALL supervision_v11_add_column('supervision_task_execution','trigger_type','VARCHAR(32) NULL');
CALL supervision_v11_add_column('supervision_task_execution','message_type_snapshot','VARCHAR(32) NULL');
CALL supervision_v11_add_column('supervision_task_execution','message_summary_snapshot','VARCHAR(1024) NULL');
CALL supervision_v11_add_column('supervision_task_execution','target_count','INT NOT NULL DEFAULT 0');
CALL supervision_v11_add_column('supervision_task_execution','success_count','INT NOT NULL DEFAULT 0');
CALL supervision_v11_add_column('supervision_task_execution','schedule_decision','VARCHAR(32) NULL');
CALL supervision_v11_add_column('supervision_task_execution','schedule_decision_reason','VARCHAR(512) NULL');
CALL supervision_v11_add_column('supervision_task_execution','snapshot_complete','TINYINT NOT NULL DEFAULT 0');

CALL supervision_v11_add_column('supervision_message_delivery','webhook_id','BIGINT NULL');
CALL supervision_v11_add_column('supervision_message_delivery','group_id_snapshot','BIGINT NULL');
CALL supervision_v11_add_column('supervision_message_delivery','group_name_snapshot','VARCHAR(128) NULL');
CALL supervision_v11_add_column('supervision_message_delivery','push_name_snapshot','VARCHAR(128) NULL');
CALL supervision_v11_add_column('supervision_message_delivery','content_summary_snapshot','VARCHAR(1024) NULL');
CALL supervision_v11_add_column('supervision_message_delivery','normalized_code','VARCHAR(64) NULL');
CALL supervision_v11_add_column('supervision_message_delivery','normalized_message','VARCHAR(512) NULL');
CALL supervision_v11_add_column('supervision_message_delivery','technical_detail_redacted','VARCHAR(1024) NULL');

CALL supervision_v11_add_column('supervision_person','gender','VARCHAR(16) NULL');
CALL supervision_v11_add_column('supervision_person','sync_status','VARCHAR(32) NOT NULL DEFAULT ''SUCCESS''');
CALL supervision_v11_add_column('supervision_person','sync_error_redacted','VARCHAR(1024) NULL');
CALL supervision_v11_add_column('supervision_person','primary_department_id','BIGINT NULL');
DROP PROCEDURE supervision_v11_add_column;

INSERT IGNORE INTO supervision_wecom_group(tenant_key,group_name,status,remark)
VALUES('default','待补充群名',1,'由旧机器人数据自动迁移，请管理员补充真实群名');

UPDATE supervision_wecom_webhook h
SET h.group_id=(SELECT MIN(g.id) FROM supervision_wecom_group g WHERE g.tenant_key='default' AND g.group_name='待补充群名')
WHERE h.group_id IS NULL;
UPDATE supervision_wecom_webhook SET push_name=name WHERE push_name IS NULL OR push_name='';
UPDATE supervision_wecom_webhook h
LEFT JOIN supervision_wechat_robot r ON r.id=h.id
SET h.system_code=COALESCE(NULLIF(r.robot_id,''),CONCAT('push-',h.id))
WHERE h.system_code IS NULL OR h.system_code='';
UPDATE supervision_task_execution SET snapshot_complete=0 WHERE task_name_snapshot IS NULL;

INSERT INTO supervision_workday_calendar_year(year_value,status,source_url,source_document,version_value,published_at,checksum_value)
VALUES
(2025,'ACTIVE','https://www.gov.cn/zhengce/zhengceku/202411/content_6986383.htm','国办发明电〔2024〕12号','2025-v1','2024-11-12','official-2025-v1'),
(2026,'ACTIVE','https://www.gov.cn/zhengce/zhengceku/202511/content_7047091.htm','国办发明电〔2025〕7号','2026-v1','2025-11-04','official-2026-v1')
ON DUPLICATE KEY UPDATE status=VALUES(status),source_url=VALUES(source_url),source_document=VALUES(source_document),version_value=VALUES(version_value),published_at=VALUES(published_at),checksum_value=VALUES(checksum_value);

-- 2025 official holiday exceptions and weekend make-up workdays.
INSERT INTO supervision_workday_calendar_exception(year_value,date_value,day_type,holiday_name,note) VALUES
(2025,'2025-01-01','HOLIDAY','元旦',NULL),
(2025,'2025-01-26','WORKDAY','春节调休','周日上班'),
(2025,'2025-01-28','HOLIDAY','春节',NULL),(2025,'2025-01-29','HOLIDAY','春节',NULL),(2025,'2025-01-30','HOLIDAY','春节',NULL),(2025,'2025-01-31','HOLIDAY','春节',NULL),(2025,'2025-02-01','HOLIDAY','春节',NULL),(2025,'2025-02-02','HOLIDAY','春节',NULL),(2025,'2025-02-03','HOLIDAY','春节',NULL),(2025,'2025-02-04','HOLIDAY','春节',NULL),
(2025,'2025-02-08','WORKDAY','春节调休','周六上班'),
(2025,'2025-04-04','HOLIDAY','清明节',NULL),(2025,'2025-04-05','HOLIDAY','清明节',NULL),(2025,'2025-04-06','HOLIDAY','清明节',NULL),
(2025,'2025-04-27','WORKDAY','劳动节调休','周日上班'),
(2025,'2025-05-01','HOLIDAY','劳动节',NULL),(2025,'2025-05-02','HOLIDAY','劳动节',NULL),(2025,'2025-05-03','HOLIDAY','劳动节',NULL),(2025,'2025-05-04','HOLIDAY','劳动节',NULL),(2025,'2025-05-05','HOLIDAY','劳动节',NULL),
(2025,'2025-05-31','HOLIDAY','端午节',NULL),(2025,'2025-06-01','HOLIDAY','端午节',NULL),(2025,'2025-06-02','HOLIDAY','端午节',NULL),
(2025,'2025-09-28','WORKDAY','国庆中秋调休','周日上班'),
(2025,'2025-10-01','HOLIDAY','国庆中秋',NULL),(2025,'2025-10-02','HOLIDAY','国庆中秋',NULL),(2025,'2025-10-03','HOLIDAY','国庆中秋',NULL),(2025,'2025-10-04','HOLIDAY','国庆中秋',NULL),(2025,'2025-10-05','HOLIDAY','国庆中秋',NULL),(2025,'2025-10-06','HOLIDAY','国庆中秋',NULL),(2025,'2025-10-07','HOLIDAY','国庆中秋',NULL),(2025,'2025-10-08','HOLIDAY','国庆中秋',NULL),
(2025,'2025-10-11','WORKDAY','国庆中秋调休','周六上班')
ON DUPLICATE KEY UPDATE day_type=VALUES(day_type),holiday_name=VALUES(holiday_name),note=VALUES(note);

-- 2026 official holiday exceptions and weekend make-up workdays.
INSERT INTO supervision_workday_calendar_exception(year_value,date_value,day_type,holiday_name,note) VALUES
(2026,'2026-01-01','HOLIDAY','元旦',NULL),(2026,'2026-01-02','HOLIDAY','元旦',NULL),(2026,'2026-01-03','HOLIDAY','元旦',NULL),(2026,'2026-01-04','WORKDAY','元旦调休','周日上班'),
(2026,'2026-02-14','WORKDAY','春节调休','周六上班'),
(2026,'2026-02-15','HOLIDAY','春节',NULL),(2026,'2026-02-16','HOLIDAY','春节',NULL),(2026,'2026-02-17','HOLIDAY','春节',NULL),(2026,'2026-02-18','HOLIDAY','春节',NULL),(2026,'2026-02-19','HOLIDAY','春节',NULL),(2026,'2026-02-20','HOLIDAY','春节',NULL),(2026,'2026-02-21','HOLIDAY','春节',NULL),(2026,'2026-02-22','HOLIDAY','春节',NULL),(2026,'2026-02-23','HOLIDAY','春节',NULL),
(2026,'2026-02-28','WORKDAY','春节调休','周六上班'),
(2026,'2026-04-04','HOLIDAY','清明节',NULL),(2026,'2026-04-05','HOLIDAY','清明节',NULL),(2026,'2026-04-06','HOLIDAY','清明节',NULL),
(2026,'2026-05-01','HOLIDAY','劳动节',NULL),(2026,'2026-05-02','HOLIDAY','劳动节',NULL),(2026,'2026-05-03','HOLIDAY','劳动节',NULL),(2026,'2026-05-04','HOLIDAY','劳动节',NULL),(2026,'2026-05-05','HOLIDAY','劳动节',NULL),(2026,'2026-05-09','WORKDAY','劳动节调休','周六上班'),
(2026,'2026-06-19','HOLIDAY','端午节',NULL),(2026,'2026-06-20','HOLIDAY','端午节',NULL),(2026,'2026-06-21','HOLIDAY','端午节',NULL),
(2026,'2026-09-20','WORKDAY','国庆调休','周日上班'),(2026,'2026-09-25','HOLIDAY','中秋节',NULL),(2026,'2026-09-26','HOLIDAY','中秋节',NULL),(2026,'2026-09-27','HOLIDAY','中秋节',NULL),
(2026,'2026-10-01','HOLIDAY','国庆节',NULL),(2026,'2026-10-02','HOLIDAY','国庆节',NULL),(2026,'2026-10-03','HOLIDAY','国庆节',NULL),(2026,'2026-10-04','HOLIDAY','国庆节',NULL),(2026,'2026-10-05','HOLIDAY','国庆节',NULL),(2026,'2026-10-06','HOLIDAY','国庆节',NULL),(2026,'2026-10-07','HOLIDAY','国庆节',NULL),(2026,'2026-10-10','WORKDAY','国庆调休','周六上班')
ON DUPLICATE KEY UPDATE day_type=VALUES(day_type),holiday_name=VALUES(holiday_name),note=VALUES(note);
