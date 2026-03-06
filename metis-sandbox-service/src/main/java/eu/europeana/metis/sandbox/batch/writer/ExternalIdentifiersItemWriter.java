package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writer implementation for persisting {@link ExecutionRecordExternalIdentifier} entities.
 *
 * <p>Processes and writes chunks of {@link ExecutionRecordExternalIdentifier} to a database repository,
 * leveraging the functionality provided by {@link RepositoryItemWriter}.
 *
 * <p>Used specifically for managing external identifiers related to execution records.
 */
@Component
@Slf4j
public class ExternalIdentifiersItemWriter extends RepositoryItemWriter<ExecutionRecordExternalIdentifier> {

  /**
   * Constructor.
   *
   * @param executionRecordExternalIdentifierRepository The repository instance used for persisting execution record external identifiers.
   */
  public ExternalIdentifiersItemWriter(ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository) {
    super(executionRecordExternalIdentifierRepository);
  }

  @Override
  public void write(Chunk<? extends ExecutionRecordExternalIdentifier> chunk) throws Exception {
    log.debug("BEGIN -> Writing chunk of {} oai identifiers to DB", chunk.size());
    super.write(chunk);
    log.debug("END -> Writing chunk of {} oai identifiers to DB", chunk.size());
  }
}
