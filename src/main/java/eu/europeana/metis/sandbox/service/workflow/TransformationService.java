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
  RecordInfo transform(Record record);

  /**
   * Transform the given record to EDM external format using a xslt given by the user previously
   *
   * @param record must not be null
   * @return {@link RecordInfo} containing record transformed
   * @throws NullPointerException      if record is null
   * @throws RecordProcessingException if records fails at transformation
   */
  RecordInfo transformToEdmExternal(Record record);

  public byte[] transformToEdmExternal(String datasetId,
      String datasetName, InputStream xsltToEdmExternal, Country country, Language language,
      byte[] recordContent);
}
