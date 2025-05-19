package eu.europeana.metis.sandbox.batch.reader;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;

@Setter
public class DefaultRepositoryItemReader extends RepositoryItemReader<ExecutionRecord> {

  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['executionId']}")
  private String executionId;

  private ExecutionRecordRepository executionRecordRepository;
  private final int chunkSize;

  public DefaultRepositoryItemReader(ExecutionRecordRepository executionRecordRepository, int chunkSize) {
    super();
    this.executionRecordRepository = executionRecordRepository;
    this.chunkSize = chunkSize;
  }

  @Override
  public void afterPropertiesSet() {

    setRepository(executionRecordRepository);
    setMethodName("findByIdentifier_DatasetIdAndIdentifier_ExecutionId");

    List<Object> queryMethodArguments = new ArrayList<>();
    queryMethodArguments.add(datasetId);
    queryMethodArguments.add(executionId);

    setArguments(queryMethodArguments);
    setPageSize(chunkSize);

    Map<String, Direction> sorts = new HashMap<>();
    sorts.put("identifier.recordId", Direction.ASC);
    setSort(sorts);
  }
}
