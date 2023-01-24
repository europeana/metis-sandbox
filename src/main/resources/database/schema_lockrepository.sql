BEGIN;
CREATE SCHEMA integration;
SET search_path TO integration;
CREATE TABLE integration.int_message
(
    message_id    CHAR(36)     NOT NULL,
    region        VARCHAR(100) NOT NULL,
    created_date  TIMESTAMP    NOT NULL,
    message_bytes BYTEA,
    CONSTRAINT int_message_pk PRIMARY KEY (message_id, region)
);

CREATE INDEX int_message_ix1 ON integration.int_message (created_date);

CREATE TABLE integration.int_group_to_message
(
    group_key  CHAR(36) NOT NULL,
    message_id CHAR(36) NOT NULL,
    region     VARCHAR(100),
    CONSTRAINT int_group_to_message_pk PRIMARY KEY (group_key, message_id, region)
);

CREATE TABLE integration.int_message_group
(
    group_key              CHAR(36)     NOT NULL,
    region                 VARCHAR(100) NOT NULL,
    group_condition        VARCHAR(255),
    complete               BIGINT,
    last_released_sequence BIGINT,
    created_date           TIMESTAMP    NOT NULL,
    updated_date           TIMESTAMP DEFAULT NULL,
    CONSTRAINT int_message_group_pk PRIMARY KEY (group_key, region)
);

CREATE TABLE integration.int_lock
(
    lock_key     CHAR(36)     NOT NULL,
    region       VARCHAR(100) NOT NULL,
    client_id    CHAR(36),
    created_date TIMESTAMP    NOT NULL,
    CONSTRAINT int_lock_pk PRIMARY KEY (lock_key, region)
);

CREATE SEQUENCE int_message_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

CREATE TABLE integration.int_channel_message
(
    message_id       CHAR(36)     NOT NULL,
    group_key        CHAR(36)     NOT NULL,
    created_date     BIGINT       NOT NULL,
    message_priority BIGINT,
    message_sequence BIGINT       NOT NULL DEFAULT NEXTVAL('int_message_seq'),
    message_bytes    BYTEA,
    region           VARCHAR(100) NOT NULL,
    CONSTRAINT int_channel_message_pk PRIMARY KEY (region, group_key, created_date, message_sequence)
);

CREATE INDEX int_channel_msg_delete_idx ON integration.int_channel_message (region, group_key, message_id);
-- this is only needed if the message group store property 'priorityenabled' is true
-- create unique index int_channel_msg_priority_idx on int_channel_message (region, group_key, message_priority desc nulls last, created_date, message_sequence);


CREATE TABLE integration.int_metadata_store
(
    metadata_key   VARCHAR(255) NOT NULL,
    metadata_value VARCHAR(4000),
    region         VARCHAR(100) NOT NULL,
    CONSTRAINT int_metadata_store_pk PRIMARY KEY (metadata_key, region)
);

-- this is only needed if using postgreschannelmessagesubscriber

/*CREATE FUNCTION int_channel_message_notify_fct()
RETURNS TRIGGER AS
 $body$
 BEGIN
     PERFORM pg_notify('int_channel_message_notify', new.region || ' ' || new.group_key);
     RETURN new;
 END;
 $body$
 LANGUAGE plpgsql;

 CREATE TRIGGER int_channel_message_notify_trg
 AFTER INSERT ON int_channel_message
 FOR EACH ROW
 EXECUTE PROCEDURE int_channel_message_notify_fct();*/
COMMIT;
