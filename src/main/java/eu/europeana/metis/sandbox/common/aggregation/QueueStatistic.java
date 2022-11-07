package eu.europeana.metis.sandbox.common.aggregation;

public class QueueStatistic {

    private final String queueName;

    private final Long countMessages;

    public QueueStatistic(String queueName, Long countMessages) {
        this.queueName = queueName;
        this.countMessages = countMessages;
    }

    public String getQueueName() {
        return queueName;
    }

    public Long getCountMessages() {
        return countMessages;
    }
}
