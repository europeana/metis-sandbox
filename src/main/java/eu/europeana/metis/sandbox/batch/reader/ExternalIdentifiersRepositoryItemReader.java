package eu.europeana.metis.sandbox.batch.reader;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;

/**
 * A Spring Batch {@link RepositoryItemReader} implementation for reading {@link ExecutionRecordExternalIdentifier} items based on
 * a target execution ID.
 *
 * <p>We are using target execution id and not a source execution id. This is because this is meant to be run as a follow-up step
 * in the same job after the external identifiers are harvested, and therefore at this point there is no source execution id
 * available for this reader.
 */
@StepScope
@Component
public class ExternalIdentifiersRepositoryItemReader extends RepositoryItemReader<ExecutionRecordExternalIdentifier> {

  private static final String REPOSITORY_QUERY_METHOD_NAME = "findByExecutionRun_ExecutionId";
  public static final String SORT_FIELD = "externalRecordId";
  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;

  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;

  /**
   * Constructor.
   *
   * @param executionRecordExternalIdentifierRepository The repository used to retrieve ExecutionRecordExternalIdentifier items.
   */
  public ExternalIdentifiersRepositoryItemReader(
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository) {
    super(executionRecordExternalIdentifierRepository, Map.of(SORT_FIELD, Direction.ASC));
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    setRepository(executionRecordExternalIdentifierRepository);
    setMethodName(REPOSITORY_QUERY_METHOD_NAME);
    setArguments(List.of(targetExecutionId));

    super.afterPropertiesSet();
  }
}
