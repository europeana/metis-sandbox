BEGIN;
CREATE SCHEMA integration;
SET search_path TO integration;

CREATE TABLE integration.int_lock
(
    lock_key     CHAR(36)     NOT NULL,
    region       VARCHAR(100) NOT NULL,
    client_id    CHAR(36),
    created_date TIMESTAMP    NOT NULL,
    CONSTRAINT int_lock_pk PRIMARY KEY (lock_key, region)
);

CREATE SEQUENCE int_message_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

COMMIT;
