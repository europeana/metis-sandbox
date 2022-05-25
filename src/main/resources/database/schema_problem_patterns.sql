BEGIN;
CREATE SCHEMA problem_patterns;

CREATE TABLE problem_patterns.execution_point
(
    execution_point_id  SERIAL,
    dataset_id          VARCHAR(20)              NOT NULL,
    execution_step      VARCHAR(20)              NOT NULL,
    execution_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (execution_point_id),
    UNIQUE (dataset_id, execution_step, execution_timestamp)
);
CREATE INDEX ON problem_patterns.execution_point (dataset_id, execution_step);

CREATE TABLE problem_patterns.dataset_problem_pattern
(
    execution_point_id INT         NOT NULL,
    pattern_id         VARCHAR(10) NOT NULL,
    record_occurrences INT         NOT NULL,
    PRIMARY KEY (execution_point_id, pattern_id),
    FOREIGN KEY (execution_point_id) REFERENCES problem_patterns.execution_point (execution_point_id)
);

CREATE TABLE problem_patterns.record_problem_pattern
(
    record_problem_pattern_id SERIAL,
    execution_point_id        INT          NOT NULL,
    record_id                 VARCHAR(255) NOT NULL,
    pattern_id                VARCHAR(10)  NOT NULL,
    PRIMARY KEY (record_problem_pattern_id),
    FOREIGN KEY (execution_point_id) REFERENCES problem_patterns.execution_point (execution_point_id),
    UNIQUE (execution_point_id, record_id, pattern_id)
);

CREATE TABLE problem_patterns.record_problem_pattern_occurrence
(
    record_problem_pattern_occurrence_id SERIAL,
    record_problem_pattern_id            INT          NOT NULL,
    message_report                       VARCHAR(255) NOT NULL,
    PRIMARY KEY (record_problem_pattern_occurrence_id),
    FOREIGN KEY (record_problem_pattern_id) REFERENCES problem_patterns.record_problem_pattern (record_problem_pattern_id),
    UNIQUE (record_problem_pattern_id, message_report)
);

CREATE TABLE problem_patterns.record_title
(
    execution_point_id INT          NOT NULL,
    record_id          VARCHAR(255) NOT NULL,
    title              VARCHAR(255) NOT NULL,
    PRIMARY KEY (execution_point_id, record_id, title),
    FOREIGN KEY (execution_point_id) REFERENCES problem_patterns.execution_point (execution_point_id)
);
COMMIT;
