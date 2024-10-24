package eu.europeana.metis.sandbox.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Contains a record and a list of errors if any
 */
public class RecordInfo {

  private final Record recordValue;
  private final List<RecordError> errors;

  /**
   * Constructor, defaults errors to an empty non-modifiable list
   *
   * @param recordValue must not be null
   */
  public RecordInfo(Record recordValue) {
    this(recordValue, List.of());
  }

  /**
   * Constructor, store errors as a list
   *
   * @param recordValue must not be null
   * @param errors must not be null
   */
  public RecordInfo(Record recordValue, List<RecordError> errors) {
    this.recordValue = recordValue;
    this.errors = Collections.unmodifiableList(errors);
  }

  public Record getRecordValue() {
    return recordValue;
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
    return recordValue.equals(that.recordValue) && errors.equals(that.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recordValue, errors);
  }

}
