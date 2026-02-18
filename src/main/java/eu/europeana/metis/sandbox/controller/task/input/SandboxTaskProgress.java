package eu.europeana.metis.sandbox.controller.task.input;

import org.springframework.batch.core.BatchStatus;

public record SandboxTaskProgress(
    long expectedRecords,
    long processedRecords,
    long successRecords,
    long failedRecords,
    long warningRecords,
    long deletedRecords,
    long duplicatedRecords,
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
