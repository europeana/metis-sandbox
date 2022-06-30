BEGIN;
CREATE SCHEMA metrics;

CREATE TABLE IF NOT EXISTS metrics.progress_per_dataset
(
    metric_id         SERIAL,
    dataset_id        VARCHAR(20) NOT NULL,
    total_records     INT         NOT NULL,
    processed_records INT         NOT NULL,
    status            VARCHAR(30) NOT NULL,
    start_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    end_timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS metrics.progress_per_step
(
    metric_id  SERIAL,
    dataset_id VARCHAR(20) NOT NULL,
    step       VARCHAR(30) NOT NULL,
    total      INT         NOT NULL,
    success    INT         NOT NULL,
    fail       INT         NOT NULL,
    warn       INT         NOT NULL
);

COMMIT;
