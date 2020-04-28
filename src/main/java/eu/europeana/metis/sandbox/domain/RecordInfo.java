package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;

public class RecordInfo {

  private final Record record;
  private final List<RecordError> errors;

  public RecordInfo(Record record) {
    this(record, List.of());
  }

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
}
