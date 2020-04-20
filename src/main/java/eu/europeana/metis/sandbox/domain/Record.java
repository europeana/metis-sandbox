package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Object that represents a record.
 * <br /><br />
 * Stores the record content as a byte[], do not
 * override the byte[] contents after object construction, we are not making a copy of it because it
 * is expensive and Record object is expected to be use as a non mutable object.
 */
public class Record {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final String recordId;
  private final String datasetId;
  private final String datasetName;
  private final Country country;
  private final Language language;
  private final byte[] content;
  private final String contentString;

  private Record(String recordId, String datasetId, String datasetName,
      Country country, Language language, byte[] content) {
    this.recordId = recordId;
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.country = country;
    this.language = language;
    this.content = content;
    this.contentString = new String(content, DEFAULT_CHARSET);
  }

  /**
   * Creates a record based on the provided record but replacing the content with the one provided
   *
   * @param record  must not be null
   * @param content must not be null. Xml representation of the record
   * @return record object
   * @throws NullPointerException if any parameter is null
   */
  public static Record from(Record record, String content) {
    requireNonNull(content);
    return from(record, content.getBytes(DEFAULT_CHARSET));
  }

  /**
   * Creates a record based on the provided record but replacing the content with the one provided
   *
   * @param record  must not be null
   * @param content must not be null. Xml representation of the record
   * @return record object
   * @throws NullPointerException if any parameter is null
   */
  public static Record from(Record record, byte[] content) {
    requireNonNull(record);
    requireNonNull(content);

    return Record.builder()
        .recordId(record.getRecordId())
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
   * @implNote Overwriting this field contents after construction will cause problems.
   * <br />
   * We are not making a copy of it because it
   * is expensive and Record object is expected to be use as a non mutable object
   */
  public byte[] getContent() {
    return this.content;
  }

  public String getContentString() {
    return contentString;
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
    return country == record.country &&
        language == record.language &&
        recordId.equals(record.recordId) &&
        datasetId.equals(record.datasetId) &&
        datasetName.equals(record.datasetName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recordId, datasetId, datasetName, country, language);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Record.class.getSimpleName() + "[", "]")
        .add("recordId='" + recordId + "'")
        .add("datasetId='" + datasetId + "'")
        .add("datasetName='" + datasetName + "'")
        .add("country=" + country)
        .add("language=" + language)
        .add("content='" + Arrays.toString(content) + "'")
        .toString();
  }

  public static class RecordBuilder {

    private String recordId;
    private String datasetId;
    private String datasetName;
    private Country country;
    private Language language;
    private byte[] content;

    public RecordBuilder recordId(String recordId) {
      this.recordId = recordId;
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
      this.content = content;
      return this;
    }

    public Record build() {
      requireNonNull(recordId, "Record id must not be null");
      requireNonNull(datasetId, "Dataset id must not be null");
      requireNonNull(datasetName, "Dataset name id must not be null");
      requireNonNull(country, "Country must not be null");
      requireNonNull(language, "Language must not be null");
      requireNonNull(content, "Content must not be null");
      return new Record(recordId, datasetId, datasetName, country, language, content);
    }
  }
}
