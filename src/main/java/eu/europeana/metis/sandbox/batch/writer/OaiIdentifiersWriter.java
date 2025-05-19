package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.stereotype.Component;

@Component
public class OaiIdentifiersWriter extends RepositoryItemWriter<ExecutionRecordExternalIdentifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public OaiIdentifiersWriter(ExecutionRecordExternalIdentifierRepository repository) {
        setRepository(repository);
    }

    @Override
    public void write(Chunk<? extends ExecutionRecordExternalIdentifier> chunk) throws Exception {
        LOGGER.info("Writing chunk of {} oai identifiers to DB", chunk.size());
        super.write(chunk);
        LOGGER.info("Chunk of {} oai identifiers written to DB", chunk.size());
    }
}
