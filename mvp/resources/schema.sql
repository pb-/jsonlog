CREATE TABLE IF NOT EXISTS logs (
    log_id VARCHAR(32) NOT NULL,
    offset BIGINT NOT NULL,
    data JSON NOT NULL,
    PRIMARY KEY (log_id, offset)
);
