package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.common.exception.RecordParsingException;

public interface XmlRecordProcessorService {

  /**
   * Extracts and return the record id of the given record <br /> Given record must be a valid xml
   * that contains an element called edm:ProvidedCHO with and attribute called rdf:about which
   * contains the record id
   *
   * @param recordContent must not be null. Xml to extract the record id
   * @return record id
   * @throws NullPointerException     if record is null
   * @throws IllegalArgumentException if record does not contain a valid id
   * @throws RecordParsingException   if fails parsing the record
   */
  String getProviderId(byte[] recordContent);
}
