package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Validation result.
 */
public class ValidationResult {
    private final Step step;
    private final List<RecordValidationMessage> messages;
    private final Status status;

    /**
     * Instantiates a new Validation result.
     *
     * @param step    the step
     * @param message the message
     * @param status  the status
     */
    public ValidationResult(Step step, RecordValidationMessage message, Status status) {
        this.step = step;
        this.messages = new ArrayList<>();
        this.messages.add(message);
        this.status = status;
    }

    /**
     * Instantiates a new Validation result.
     *
     * @param step     the step
     * @param messages the messages
     * @param status   the status
     */
    public ValidationResult(Step step, List<RecordValidationMessage> messages, Status status) {
        this.step = step;
        this.messages = new ArrayList<>();
        this.messages.addAll(messages);
        this.status = status;
    }

    /**
     * Gets validation messages.
     *
     * @return the validation messages
     */
    public List<RecordValidationMessage> getMessages() {
        return messages;
    }

    /**
     * Add message.
     *
     * @param message the message
     */
    public void addMessage(RecordValidationMessage message) {
        this.messages.add(message);
    }

    /**
     * Gets step.
     *
     * @return the step
     */
    public Step getStep() {
        return step;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * The enum Status.
     */
    public enum Status {
        /**
         * Passed status.
         */
        PASSED,
        /**
         * Failed status.
         */
        FAILED
    }
}
