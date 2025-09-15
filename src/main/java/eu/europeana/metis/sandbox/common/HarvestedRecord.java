package eu.europeana.metis.sandbox.common;

/**
 * Represents a record containing information about a harvested item.
 *
 * @param sourceRecordId unique identifier representing the source of the record.
 * @param recordId unique identifier for the harvested record.
 * @param recordData raw data content of the harvested record.
 */
public record HarvestedRecord(String sourceRecordId, String recordId, String recordData) {

}
