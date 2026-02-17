package eu.europeana.metis.sandbox.controller.task.input;

public record SandboxTaskProgress(
    long expectedRecords,
    long processedRecords,
    long successRecords,
    long failedRecords,
    long warningRecords,
    long deletedRecords,
    long duplicatedRecords) {

}
