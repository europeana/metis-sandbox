package eu.europeana.metis.sandbox.dto.validation;

public record RecordValidationMessage(Type messageType,
                                      String message) {
    public enum Type {
        INFO, WARN, ERROR
    }
}
