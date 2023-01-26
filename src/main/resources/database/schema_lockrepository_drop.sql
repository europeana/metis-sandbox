BEGIN;
-- SET search_path TO integration;
-- DROP TABLE IF EXISTS int_message
-- DROP TABLE IF EXISTS int_group_to_message
-- DROP TABLE IF EXISTS int_message_group
-- DROP TABLE IF EXISTS int_lock
-- DROP TABLE IF EXISTS int_channel_message
-- DROP TABLE IF EXISTS int_metadata_store
-- DROP SEQUENCE IF EXISTS int_message_seq
DROP SCHEMA IF EXISTS integration CASCADE;
COMMIT;
