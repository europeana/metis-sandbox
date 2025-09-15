CREATE SCHEMA IF NOT EXISTS public;
CREATE SCHEMA IF NOT EXISTS engine_record;
CREATE SCHEMA IF NOT EXISTS problem_patterns;
CREATE SCHEMA IF NOT EXISTS integration;
CREATE SCHEMA IF NOT EXISTS rate_limit;

CREATE TABLE IF NOT EXISTS rate_limit.buckets
(
    id            BIGINT    NOT NULL,
    bucket_state  BYTEA     NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS integration.int_lock
(
    lock_key     CHAR(36)     NOT NULL,
    region       VARCHAR(100) NOT NULL,
    client_id    CHAR(36),
    created_date TIMESTAMP    NOT NULL,
    CONSTRAINT int_lock_pk PRIMARY KEY (lock_key, region)
);