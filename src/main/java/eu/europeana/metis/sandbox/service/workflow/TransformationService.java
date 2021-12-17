package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.io.InputStream;

public interface TransformationService {

  /**
   * Transform the given record using the default xslt
   *
   * @param record must not be null
   * @return {@link RecordInfo} containing record transformed
   * @throws NullPointerException      if record is null
   * @throws RecordProcessingException if records fails at transformation
   */
  RecordInfo transformToEdmInternal(Record record);

  /**
   * Transform the given record to EDM external format using a xslt given by the user previously
   *
   * @param datasetId The id of the dataset where record belongs to
   * @param xsltToEdmExternal The xslt content to perform the transformation
   * @param recordContent The content of the record to be transformed as an array of bytes
   * @return {@link RecordInfo} containing record transformed
   * @throws NullPointerException      if record is null
   * @throws RecordProcessingException if records fails at transformation
   */

  byte[] transform(String datasetId, InputStream xsltToEdmExternal, byte[] recordContent);
}
