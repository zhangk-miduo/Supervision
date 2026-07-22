-- Read-only ownership migration report for V12. Run after taking a database backup.
SELECT 'task' AS record_type, COUNT(*) AS total,
       SUM(owner_account_id IS NOT NULL) AS assigned,
       SUM(owner_account_id IS NULL) AS unassigned
FROM supervision_task
UNION ALL
SELECT 'execution', COUNT(*), SUM(owner_account_id IS NOT NULL), SUM(owner_account_id IS NULL)
FROM supervision_task_execution
UNION ALL
SELECT 'webhook', COUNT(*), SUM(owner_account_id IS NOT NULL), SUM(owner_account_id IS NULL)
FROM supervision_wecom_webhook
UNION ALL
SELECT 'wecom_group', COUNT(*), SUM(owner_account_id IS NOT NULL), SUM(owner_account_id IS NULL)
FROM supervision_wecom_group;

SELECT t.id, t.name, t.created_by AS legacy_creator
FROM supervision_task t
WHERE t.owner_account_id IS NULL
ORDER BY t.id;

SELECT h.id, h.push_name, h.name
FROM supervision_wecom_webhook h
WHERE h.owner_account_id IS NULL
ORDER BY h.id;

SELECT e.id, e.task_id, e.task_name_snapshot
FROM supervision_task_execution e
WHERE e.owner_account_id IS NULL
ORDER BY e.id;
