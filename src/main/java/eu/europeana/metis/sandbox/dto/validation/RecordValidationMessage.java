package eu.europeana.metis.sandbox.dto.validation;

/**
 * Represents a validation message with a specific severity type and message.
 */
public record RecordValidationMessage(Type messageType,
                                      String message) {

    /**
     * Represents the different levels of message severity.
     */
    public enum Type {
        INFO, WARN, ERROR
    }
}
