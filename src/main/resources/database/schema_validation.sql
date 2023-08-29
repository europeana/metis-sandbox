BEGIN;
CREATE SCHEMA rate_limit;

CREATE TABLE IF NOT EXISTS rate_limit.buckets
(
    id            BIGINT    NOT NULL,
    bucket_state  BYTEA     NOT NULL,
    user_address  CHAR(15)  NOT NULL,
    PRIMARY KEY (id)
);

COMMIT;
