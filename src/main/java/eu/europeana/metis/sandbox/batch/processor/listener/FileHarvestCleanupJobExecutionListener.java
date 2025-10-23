package eu.europeana.metis.sandbox.batch.processor.listener;

import eu.europeana.metis.sandbox.service.workflow.harvest.FileHarvestService;
import java.io.IOException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Listener that deletes a specified temporary directory after the batch job finishes.
 */
@Slf4j
@JobScope
@Component
public class FileHarvestCleanupJobExecutionListener implements JobExecutionListener {

  @Value("#{jobParameters['datasetId']}")
  private String datasetId;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.debug("File harvest job starting...");
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    Path pathToTempDestinationDirectoryById = FileHarvestService.getPathToTempDestinationDirectoryById(datasetId);
    try {
      FileUtils.deleteDirectory(pathToTempDestinationDirectoryById.toFile());
    } catch (IOException e) {
      log.error("Error deleting temporary directory {}", pathToTempDestinationDirectoryById, e);
    }
  }
}

