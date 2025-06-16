package eu.europeana.metis.sandbox.batch.reader;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;

/**
 * A Spring Batch {@link RepositoryItemReader} implementation for reading {@link ExecutionRecordExternalIdentifier} items based on
 * a target execution ID.
 *
 * <p>We are using target execution id and not a source execution id. This is because this is meant to be run as a follow-up step
 * in the same job after the oai identifiers are harvested, and therefore there is no source execution id available for this
 * reader.
 */
@StepScope
@Component
public class OaiIdentifiersRepositoryItemReader extends RepositoryItemReader<ExecutionRecordExternalIdentifier> {

  private static final String REPOSITORY_QUERY_METHOD_NAME = "findByIdentifier_ExecutionId";
  public static final String SORT_FIELD = "identifier.sourceRecordId";
  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;

  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;

  public OaiIdentifiersRepositoryItemReader(
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository) {
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    setRepository(executionRecordExternalIdentifierRepository);
    setSort(Collections.emptyMap());
    setMethodName(REPOSITORY_QUERY_METHOD_NAME);
    setArguments(List.of(targetExecutionId));
    Map<String, Direction> sorts = new HashMap<>();
    sorts.put(SORT_FIELD, Direction.ASC);
    setSort(sorts);

    super.afterPropertiesSet();
  }
}
