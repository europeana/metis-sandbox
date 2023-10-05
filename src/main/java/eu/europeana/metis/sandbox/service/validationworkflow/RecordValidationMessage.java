package eu.europeana.metis.sandbox.service.validationworkflow;

public class RecordValidationMessage {
    private final String message;
    private final Type messageType;

    public RecordValidationMessage(Type messageType, String message) {
        this.message = message;
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public Type getMessageType() {
        return messageType;
    }

    public enum Type {
        INFO,
        WARN,
        ERROR
    }

}
