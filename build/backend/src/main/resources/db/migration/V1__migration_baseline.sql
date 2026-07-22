CREATE TABLE IF NOT EXISTS supervision_schema_marker (
    id INT PRIMARY KEY,
    description VARCHAR(128) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO supervision_schema_marker(id, description) VALUES (1, 'Flyway migration baseline');