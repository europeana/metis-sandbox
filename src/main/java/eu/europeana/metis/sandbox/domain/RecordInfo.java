package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Contains a record and a list of errors if any
 */
public class RecordInfo {

  private final Record record;
  private final List<RecordError> errors;

  /**
   * Constructor, defaults errors to an empty non modifiable list
   *
   * @param record must not be null
   * @throws NullPointerException if record is null
   */
  public RecordInfo(Record record) {
    this(record, List.of());
  }

  /**
   * Constructor, store errors as a non modifiable list
   *
   * @param record must not be null
   * @param errors must not be null
   * @throws NullPointerException if any parameter is null
   */
  public RecordInfo(Record record,
      List<RecordError> errors) {
    requireNonNull(record, "Record must not be null");
    requireNonNull(errors, "Errors must not be null");
    this.record = record;
    this.errors = Collections.unmodifiableList(errors);
  }

  public Record getRecord() {
    return record;
  }

  public List<RecordError> getErrors() {
    return errors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordInfo that = (RecordInfo) o;
    return record.equals(that.record) && errors.equals(that.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(record, errors);
  }

}
