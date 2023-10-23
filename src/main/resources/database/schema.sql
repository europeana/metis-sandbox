BEGIN;
CREATE TABLE IF NOT EXISTS dataset
(
    dataset_id                SERIAL,
    dataset_name              VARCHAR(255) NOT NULL,
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

CREATE TABLE IF NOT EXISTS record
(
    id                                      BIGSERIAL,
    europeana_id                            VARCHAR(255) NULL,
    provider_id                             VARCHAR(255) NULL,
    dataset_id                              VARCHAR(100) NOT NULL,
    content_tier                            VARCHAR(3) NULL,
    content_tier_before_license_correction  VARCHAR(3) NULL,
    metadata_tier                           VARCHAR(3) NULL,
    metadata_tier_language                  VARCHAR(3) NULL,
    metadata_tier_enabling_elements         VARCHAR(3) NULL,
    metadata_tier_contextual_classes        VARCHAR(3) NULL,
    license                                 VARCHAR(13) NULL,
    PRIMARY KEY (id),
    UNIQUE (europeana_id, dataset_id),
    UNIQUE (provider_id, dataset_id)
);

CREATE TABLE IF NOT EXISTS record_log
(
    id           BIGSERIAL,
    record_id    BIGINT      NOT NULL,
    step         VARCHAR(30) NOT NULL,
    status       VARCHAR(30) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    content      TEXT        NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (record_id) REFERENCES record (id),
    UNIQUE (record_id, step)
);

CREATE TABLE IF NOT EXISTS record_error_log
(
    id           BIGSERIAL,
    record_id    BIGINT      NOT NULL,
    step         VARCHAR(30) NOT NULL,
    status       VARCHAR(30) NOT NULL,
    message      TEXT        NOT NULL,
    stack_trace  TEXT        NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (id),
    FOREIGN KEY (record_id) REFERENCES record (id)
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

CREATE TABLE IF NOT EXISTS harvesting_parameters
(
    id               BIGSERIAL,
    dataset_id       BIGINT      NOT NULL,
    protocol         VARCHAR(10) NOT NULL,
    file_name        TEXT        NULL,
    file_type        VARCHAR(9)  NULL,
    url              TEXT        NULL,
    set_spec         TEXT        NULL,
    metadata_format  TEXT        NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (dataset_id) REFERENCES dataset (dataset_id)
    );

CREATE INDEX ON harvesting_parameters (dataset_id);
CREATE INDEX ON dataset_log (dataset_id);
CREATE INDEX ON record_log (record_id);
CREATE INDEX ON record_error_log (record_id);
CREATE INDEX ON record (dataset_id, europeana_id, provider_id);
CREATE INDEX ON record (dataset_id, content_tier);
CREATE INDEX ON record (dataset_id, metadata_tier);
CREATE INDEX ON thumbnail (dataset_id);
COMMIT;
