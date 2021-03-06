/*
 Tables definitions for PostgreSQL database
 */

begin;
drop table if exists dataset;
drop table if exists record_log;
drop table if exists record_error_log;
drop table if exists thumbnail;
commit;

begin;
create table if not exists dataset (
   dataset_id serial,
   dataset_name varchar(255) not null,
   records_quantity integer not null,
   created_date timestamp with time zone default now(),
   Primary Key (dataset_id)
);

create table  if not exists record_log (
   id bigserial,
   record_id varchar(100) not null,
   dataset_id varchar(100) not null,
   step varchar(30) not null,
   status varchar(30) not null,
   content text not null,
   created_date timestamp with time zone default now(),
   Primary Key(id),
   unique (record_id, dataset_id, step)
);

create table if not exists record_error_log (
    id bigserial,
    record_id varchar(100) not null,
    dataset_id varchar(100) not null ,
    step varchar(30) not null,
    status varchar(30) not null,
    message text not null ,
    stack_trace text not null ,
    created_date timestamp with time zone default now(),
    Primary Key (id)
);

create table if not exists thumbnail (
    id bigserial,
    dataset_id varchar(100) not null ,
    thumbnail_id varchar(200) not null,
    Primary Key (id)
);

create index on record_log (dataset_id);
create index on record_error_log (dataset_id);
create index on thumbnail (dataset_id);
commit;