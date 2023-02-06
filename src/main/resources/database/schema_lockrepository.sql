BEGIN;
CREATE SCHEMA integration;

CREATE TABLE integration.int_lock
(
    lock_key     CHAR(36)     NOT NULL,
    region       VARCHAR(100) NOT NULL,
    client_id    CHAR(36),
    created_date TIMESTAMP    NOT NULL,
    CONSTRAINT int_lock_pk PRIMARY KEY (lock_key, region)
);

COMMIT;
