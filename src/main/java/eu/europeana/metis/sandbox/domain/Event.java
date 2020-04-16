package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import java.util.Optional;

/**
 * Event that contains a record and its processing details
 */
public class Event {

  private final Record body;
  private final Status status;
  private final Step step;

  private final EventError eventError;

  /**
   * Creates an event based on the one provided, using the provided {@link Step}
   * @param body must not be null
   * @param step must not be null
   * @throws NullPointerException if any parameter is null
   */
  public Event(Record body, Step step) {
    this(body, step, null);
  }

  /**
   * Creates an event based on the one provided, using the provided {@link Step}
   * @param body must not be null
   * @param step must not be null
   * @param eventError could be null
   * @throws NullPointerException if body or step are null
   */
  public Event(Record body, Step step, EventError eventError) {
    requireNonNull(body, "Body must not be null");
    requireNonNull(step, "Step must not be null");
    this.status = eventError == null ? Status.SUCCESS : Status.FAIL;
    this.body = body;
    this.eventError = eventError;
    this.step = step;
  }

  public Record getBody() {
    return body;
  }

  public Status getStatus() {
    return status;
  }

  public Step getStep() {
    return step;
  }

  public Optional<EventError> getEventError() {
    return Optional.ofNullable(eventError);
  }
}
