package eu.europeana.metis.sandbox.service.validationworkflow;

/**
 * The type Validation result.
 */
public class ValidationResult {
    private final String message;
    private final Status status;

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

    /**
     * Instantiates a new Validation result.
     *
     * @param message the message
     * @param status  the status
     */
    public ValidationResult(String message, Status status) {
        this.message = message;
        this.status = status;
    }

    /**
     * Gets message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }
}
