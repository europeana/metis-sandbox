BEGIN;
CREATE SCHEMA rate_limit;

CREATE TABLE IF NOT EXISTS rate_limit.buckets
(
    id            BIGINT    NOT NULL,
    bucket_state  BYTEA     NOT NULL,
    PRIMARY KEY (id)
);

COMMIT;
