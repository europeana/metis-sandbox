package eu.europeana.metis.sandbox.common.task.input;

import org.springframework.batch.core.BatchStatus;

/**
 * Represents the progress statistics of a sandbox task, detailing records processed, success, failure, warnings, duplicates, and
 * other states linked to processing and depublishing activities.
 *
 * @param expectedRecords The total number of records expected to be processed.
 * @param processedRecords The total number of records that have been processed so far.
 * @param successRecords The total number of records successfully processed.
 * @param failRecords The total number of records that failed during processing.
 * @param warningRecords The total number of records that caused warnings during processing.
 * @param duplicateRecords The total number of duplicate records detected during processing.
 * @param expectedDepublishRecords The total number of records expected to be depublished.
 * @param successDepublishRecords The total number of records successfully depublished.
 * @param failDepublishRecords The total number of records that failed during the depublishing process.
 * @param processedDepublishRecords The total number of records that have been processed for depublishing so far.
 * @param sandboxTaskState The current state of the sandbox task.
 */
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

  /**
   * Enum representing the state of a sandbox task during its lifecycle.
   */
  public enum SandboxTaskState {
    RUNNING, FINISHED, CANCELLED, FAILED;

    /**
     * Converts a {@link BatchStatus} to the corresponding {@code SandboxTaskState}. The mapping is performed based on the
     * lifecycle state of the batch process.
     *
     * @param batchStatus The {@code BatchStatus} representing the current state of a batch process.
     * @return The corresponding {@code SandboxTaskState} which maps to the given {@code BatchStatus}.
     */
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
