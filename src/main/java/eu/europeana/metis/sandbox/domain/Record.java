package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * Object that represents a record. <br /><br /> Stores the record content as a byte[], do not override the byte[] contents after
 * object construction, we are not making a copy of it because it is expensive and Record object is expected to be use as a non
 * mutable object.
 */
public final class Record {

  private final String recordId;
  private String europeanaId;
  private String providerId;
  private final String datasetId;
  private final String datasetName;
  private final Country country;
  private final Language language;
  private final byte[] content;

  //Suppress: Mutable members should not be stored or returned directly
  //byte[] coming from RecordBuilder is already a copy of the original byte[]
  @SuppressWarnings("squid:S2384")
  private Record(String recordId, String europeanaId, String providerId, String datasetId,
      String datasetName,
      Country country, Language language, byte[] content) {
    this.recordId = recordId;
    this.providerId = providerId;
    this.europeanaId = europeanaId;
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.country = country;
    this.language = language;
    this.content = content;
  }

  /**
   * Creates a record based on the provided record but replacing the content with the one provided
   *
   * @param record must not be null
   * @param content must not be null. Xml representation of the record
   * @return record object
   */
  public static Record from(Record record, byte[] content) {

    return Record.builder()
        .recordId(record.getRecordId())
        .europeanaId(record.getEuropeanaId())
        .datasetId(record.getDatasetId())
        .datasetName(record.getDatasetName())
        .content(content)
        .country(record.getCountry())
        .language(record.getLanguage())
        .build();
  }

  public static RecordBuilder builder() {
    return new RecordBuilder();
  }

  public String getRecordId() {
    return this.recordId;
  }

  public String getEuropeanaId() {
    return this.europeanaId;
  }

  public void setEuropeanaId(String europeanaId) {
    this.europeanaId = europeanaId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public String getProviderId(){
    return this.providerId;
  }

  public String getDatasetId() {
    return this.datasetId;
  }

  public String getDatasetName() {
    return this.datasetName;
  }

  public Country getCountry() {
    return this.country;
  }

  public Language getLanguage() {
    return this.language;
  }

  /**
   * Content of the record
   *
   * @implNote Overwriting this field contents after construction could cause problems. <br /> We are not making a copy of it
   * because it is expensive and Record object is expected to be use as a non mutable object
   */
  //Suppress: Mutable members should not be stored or returned directly
  @SuppressWarnings("squid:S2384")
  public byte[] getContent() {
    return this.content;
  }

  /**
   * Content of the record
   *
   * @return InputStream containing the record
   */
  public InputStream getContentInputStream() {
    return new ByteArrayInputStream(content);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Record record = (Record) o;
    return
        recordId.equals(record.recordId) &&
            datasetId.equals(record.datasetId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recordId, datasetId);
  }

  public static class RecordBuilder {

    private String recordId;
    private String providerId;
    private String europeanaId;
    private String datasetId;
    private String datasetName;
    private Country country;
    private Language language;
    private byte[] content;

    public RecordBuilder recordId(String recordId) {
      this.recordId = recordId;
      return this;
    }

    public RecordBuilder europeanaId(String europeanaId) {
      this.europeanaId = europeanaId;
      return this;
    }

    public RecordBuilder providerId(String providerId){
      this.providerId = providerId;
      return this;
    }

    public RecordBuilder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public RecordBuilder datasetName(String datasetName) {
      this.datasetName = datasetName;
      return this;
    }

    public RecordBuilder country(Country country) {
      this.country = country;
      return this;
    }

    public RecordBuilder language(Language language) {
      this.language = language;
      return this;
    }

    public RecordBuilder content(byte[] content) {
      requireNonNull(content, "Content must not be null");
      this.content = Arrays.copyOf(content, content.length);
      return this;
    }

    public Record build() {
      return new Record(recordId, europeanaId, providerId, datasetId, datasetName, country, language, content);
    }
  }
}
