package eu.europeana.metis.sandbox.batch.reader;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;

/**
 * A specialized implementation of {@link RepositoryItemReader} that reads {@link ExecutionRecord} items from a repository based
 * on dataset ID and source execution ID.
 */
public class DefaultRepositoryItemReader extends RepositoryItemReader<ExecutionRecord> {

  private static final String REPOSITORY_QUERY_METHOD_NAME = "findByIdentifier_DatasetIdAndIdentifier_ExecutionId";
  public static final String SORT_FIELD = "identifier.recordId";
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['sourceExecutionId']}")
  private String sourceExecutionId;

  private ExecutionRecordRepository executionRecordRepository;
  private final int chunkSize;

  /**
   * Constructs a new instance of DefaultRepositoryItemReader with the specified repository and chunk size.
   *
   * @param executionRecordRepository The repository used to retrieve ExecutionRecord items.
   * @param chunkSize The size of the chunks to be processed.
   */
  public DefaultRepositoryItemReader(ExecutionRecordRepository executionRecordRepository, int chunkSize) {
    super();
    this.executionRecordRepository = executionRecordRepository;
    this.chunkSize = chunkSize;
  }

  @Override
  public void afterPropertiesSet() {
    setRepository(executionRecordRepository);
    setMethodName(REPOSITORY_QUERY_METHOD_NAME);

    List<Object> queryMethodArguments = new ArrayList<>();
    queryMethodArguments.add(datasetId);
    queryMethodArguments.add(sourceExecutionId);

    setArguments(queryMethodArguments);
    setPageSize(chunkSize);

    Map<String, Direction> sorts = new HashMap<>();
    sorts.put(SORT_FIELD, Direction.ASC);
    setSort(sorts);
  }
}
