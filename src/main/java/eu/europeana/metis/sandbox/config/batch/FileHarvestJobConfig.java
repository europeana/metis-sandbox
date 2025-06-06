package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_FILE;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.TimestampJobParametersIncrementer;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.reader.FileItemReader;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class FileHarvestJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String STEP_NAME = "fileHarvestStep";
  public static final BatchJobType BATCH_JOB = HARVEST_FILE;

  @Value("${fileHarvest.chunkSize:5}")
  public int chunkSize;
  @Value("${fileHarvest.parallelizationSize:1}")
  public int parallelization;

  //This can alternatively be divided similarly to oai. First read all file names and store them in db and then as a second step process those files(ids)
  @Bean
  public Job fileHarvestJob(JobRepository jobRepository,
      @Qualifier(STEP_NAME) Step fileHarvestStep) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .incrementer(new TimestampJobParametersIncrementer())
        .start(fileHarvestStep)
        .build();
  }

  @Bean(STEP_NAME)
  public Step fileHarvestStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      FileItemReader fileItemReader,
      ItemWriter<ExecutionRecordDTO> executionRecordWriter) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecordDTO, ExecutionRecordDTO>chunk(chunkSize, transactionManager)
        .reader(fileItemReader)
        .writer(executionRecordWriter)
        .build();
  }
}
