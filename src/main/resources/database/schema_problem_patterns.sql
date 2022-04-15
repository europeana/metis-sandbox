begin;
-- drop table if exists record_title;
-- drop table if exists record_problem_pattern_occurence;
-- drop table if exists record_problem_pattern;
-- drop table if exists dataset_problem_pattern;
-- drop table if exists execution_point;
drop schema if exists problem_patterns cascade;
commit;

begin;
create schema problem_patterns;

create table problem_patterns.execution_point
(
    execution_point_id  serial,
    dataset_id          varchar(20)              not null,
    execution_step      varchar(20)              not null,
    execution_timestamp timestamp with time zone not null,
    Primary Key (execution_point_id),
    unique(dataset_id, execution_step, execution_timestamp)
);

create table problem_patterns.dataset_problem_pattern
(
    execution_point_id int         not null,
    pattern_id         varchar(10) not null,
    record_occurrences  int         not null,
    Primary Key (execution_point_id, pattern_id),
    Foreign Key (execution_point_id) References problem_patterns.execution_point (execution_point_id)
);

create table problem_patterns.record_problem_pattern
(
    record_problem_pattern_id serial,
    execution_point_id        int          not null,
    record_id                 varchar(255) not null,
    pattern_id                varchar(10)  not null,
    Primary Key (record_problem_pattern_id),
    Foreign Key (execution_point_id) References problem_patterns.execution_point (execution_point_id),
    unique (execution_point_id, record_id, pattern_id)
);

create table problem_patterns.record_problem_pattern_occurrence
(
    record_problem_pattern_occurrence_id serial,
    record_problem_pattern_id           int          not null,
    message_report                      varchar(255) not null,
    Primary Key (record_problem_pattern_occurrence_id),
    Foreign Key (record_problem_pattern_id) References problem_patterns.record_problem_pattern (record_problem_pattern_id),
    unique (record_problem_pattern_id, message_report)
);
--
-- create table record_title
-- (
--     execution_point_id int          not null,
--     record_id          varchar(255) not null,
--     title              varchar(255) not null,
--     Primary Key (execution_point_id, record_id, title),
--     Foreign Key (execution_point_id) References execution_point (execution_point_id)
-- );
commit;