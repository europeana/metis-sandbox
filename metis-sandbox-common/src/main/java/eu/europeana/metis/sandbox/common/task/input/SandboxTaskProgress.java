package eu.europeana.metis.sandbox.common.task.input;

import org.springframework.batch.core.BatchStatus;

public record SandboxTaskProgress(
    long expectedRecords,
    long processedRecords,
    long successRecords,
    long failRecords,
    long warningRecords,
    long duplicateRecords,
    long expectedDepublishRecords,
    long successDepublishRecords,
    long failDepublishRecords,
    long processedDepublishRecords,
    SandboxTaskState sandboxTaskState) {

  public enum SandboxTaskState {
    RUNNING, FINISHED, CANCELLED, FAILED;

    public static SandboxTaskState fromBatchStatus(BatchStatus batchStatus) {
      return switch (batchStatus) {
        case STARTING, STARTED -> RUNNING;
        case STOPPING, STOPPED, ABANDONED -> CANCELLED;
        case COMPLETED -> FINISHED;
        default -> FAILED;
      };
    }
  }

}
