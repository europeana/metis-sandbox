BEGIN;
DROP TABLE IF EXISTS record_debias_detail;
DROP TABLE IF EXISTS record_debias_main;
DROP TABLE IF EXISTS dataset_debias_detect;
DROP TABLE IF EXISTS dataset_log;
DROP TABLE IF EXISTS harvesting_parameter;
DROP TABLE IF EXISTS dataset;
DROP TABLE IF EXISTS record_log;
DROP TABLE IF EXISTS record_error_log;
DROP TABLE IF EXISTS record;
DROP TABLE IF EXISTS thumbnail;
DROP TABLE IF EXISTS default_transform_xslt;

DROP SCHEMA if EXISTS "batch-framework" CASCADE;
COMMIT;
