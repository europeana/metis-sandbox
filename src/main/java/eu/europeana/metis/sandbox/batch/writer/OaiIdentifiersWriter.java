package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writer implementation for persisting {@link ExecutionRecordExternalIdentifier} entities.
 *
 * <p>Processes and writes chunks of {@link ExecutionRecordExternalIdentifier} to a database repository,
 * leveraging the functionality provided by {@link RepositoryItemWriter}.
 *
 * <p>Used specifically for managing external identifiers related to execution records, such as during OAI harvesting.
 */
@Component
@Slf4j
public class OaiIdentifiersWriter extends RepositoryItemWriter<ExecutionRecordExternalIdentifier> {

  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;

  /**
   * Constructor.
   *
   * @param executionRecordExternalIdentifierRepository The repository instance used for persisting execution record external identifiers.
   */
  public OaiIdentifiersWriter(ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository) {
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
  }

  /**
   * Initializes the item writer by setting the repository for writing execution record identifier items.
   */
  @PostConstruct
  public void initialize() {
    setRepository(executionRecordExternalIdentifierRepository);
  }

  @Override
  public void write(Chunk<? extends ExecutionRecordExternalIdentifier> chunk) throws Exception {
    log.info("Writing chunk of {} oai identifiers to DB", chunk.size());
    super.write(chunk);
    log.info("Chunk of {} oai identifiers written to DB", chunk.size());
  }
}
