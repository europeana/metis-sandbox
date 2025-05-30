BEGIN;
CREATE TABLE IF NOT EXISTS dataset
(
    dataset_id                SERIAL,
    dataset_name              VARCHAR(255) NOT NULL,
    created_by_id             VARCHAR(100) NULL,
    records_quantity          INTEGER      NULL,
    created_date              TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    country                   VARCHAR(35)  NOT NULL,
    language                  VARCHAR(3)   NOT NULL,
    record_limit_exceeded     bool,
    xslt_edm_external_content TEXT,
    PRIMARY KEY (dataset_id)
);

CREATE TABLE IF NOT EXISTS dataset_log
(
    id           BIGSERIAL,
    dataset_id   BIGINT      NOT NULL,
    status       VARCHAR(30) NOT NULL,
    message      TEXT        NOT NULL,
    stack_trace  TEXT        NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (id),
    FOREIGN KEY (dataset_id) REFERENCES dataset (dataset_id)
);

CREATE TABLE IF NOT EXISTS thumbnail
(
    id           BIGSERIAL,
    dataset_id   VARCHAR(100) NOT NULL,
    thumbnail_id VARCHAR(200) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS default_transform_xslt
(
    id             SERIAL,
    transform_xslt TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS harvesting_parameter
(
    id               BIGSERIAL,
    dataset_id       BIGINT      NOT NULL,
    harvest_protocol VARCHAR(10) NOT NULL,
    file_name        TEXT        NULL,
    file_type        VARCHAR(9)  NULL,
    url              TEXT        NULL,
    set_spec         TEXT        NULL,
    metadata_format  TEXT        NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (dataset_id) REFERENCES dataset (dataset_id)
);

CREATE TABLE IF NOT EXISTS dataset_debias_detect
(
    id           BIGSERIAL,
    dataset_id   BIGINT      NOT NULL,
    state        VARCHAR(30) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (id),
    FOREIGN KEY (dataset_id) REFERENCES dataset (dataset_id)
);

CREATE TABLE IF NOT EXISTS record_debias_main
(
    id           BIGSERIAL,
    record_id    BIGINT      NOT NULL,
    literal      TEXT        NOT NULL,
    language     VARCHAR(2)  NOT NULL,
    source_field VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (record_id) REFERENCES record (id)
);

CREATE TABLE IF NOT EXISTS record_debias_detail
(
    id           BIGSERIAL,
    debias_id    BIGINT      NOT NULL,
    tag_start    INT         NOT NULL,
    tag_end      INT         NOT NULL,
    tag_length   INT         NOT NULL,
    tag_uri      TEXT        NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (debias_id) REFERENCES record_debias_main (id)
);

CREATE INDEX ON record_debias_detail (debias_id);
CREATE INDEX ON record_debias_main (record_id, source_field, language);
CREATE INDEX ON dataset_debias_detect (dataset_id);
CREATE INDEX ON harvesting_parameter (dataset_id);
CREATE INDEX ON dataset_log (dataset_id);
CREATE INDEX ON thumbnail (dataset_id);

CREATE SCHEMA "batch-framework";

COMMIT;
