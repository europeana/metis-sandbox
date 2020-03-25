create table dataset
(
    dataset_id varchar(255) generated by default as identity,
    dataset_name varchar(255),
    records_quantity integer,
    primary key (dataset_id)
);

create table record_log
(
    dataset_id varchar(255) not null,
    id varchar(255) not null,
    step varchar(255) not null,
    content VARCHAR(MAX),
    error VARCHAR(MAX),
    result varchar(255),
    primary key (dataset_id, id, step)
)