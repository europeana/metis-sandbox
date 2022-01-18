/*
 Tables definitions for PostgresSQL database
 */

begin;
drop table if exists dataset;
drop table if exists record_log;
drop table if exists record_error_log;
drop table if exists record;
drop table if exists thumbnail;
drop table if exists default_transform_xslt;
commit;

begin;
create table if not exists dataset (
    dataset_id serial,
    dataset_name varchar(255) not null,
    records_quantity integer not null,
    created_date timestamp with time zone default now(),
    country varchar(35) not null,
    language varchar(3) not null,
    record_limit_exceeded bool,
    xslt_edm_external_content text,
    Primary Key (dataset_id)
    );

create table if not exists record
(
    id bigserial,
    europeana_id varchar(100) null,
    provider_id varchar(100) null,
    dataset_id varchar(100) not null,
    content text not null,
    Primary Key (id)
    );

create table  if not exists record_log (
    id bigserial,
    record_id bigint not null,
    step varchar(30) not null,
    status varchar(30) not null,
    created_date timestamp with time zone default now(),
    Primary Key(id),
    Foreign Key (record_id) References record(id)
    );

create table if not exists record_error_log (
    id bigserial,
    record_id bigint not null,
    step varchar(30) not null,
    status varchar(30) not null,
    message text not null,
    stack_trace text not null ,
    created_date timestamp with time zone default now(),
    Primary Key (id),
    Foreign Key (record_id) References record(id)
    );

create table if not exists thumbnail (
    id bigserial,
    dataset_id varchar(100) not null ,
    thumbnail_id varchar(200) not null,
    Primary Key (id)
    );

create table if not exists default_transform_xslt
(
    id             serial,
    transform_xslt text not null,
    Primary Key (id)
    );

create index on record_log (record_id);
create index on record_error_log (record_id);
create index on record (dataset_id);
create index on thumbnail (dataset_id);
commit;