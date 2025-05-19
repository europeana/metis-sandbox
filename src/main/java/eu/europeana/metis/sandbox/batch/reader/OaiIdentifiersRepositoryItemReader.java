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

@Component
@StepScope
public class OaiIdentifiersRepositoryItemReader extends RepositoryItemReader<ExecutionRecordExternalIdentifier> {

    @Value("#{jobParameters['overrideJobId'] ?: stepExecution.jobExecution.jobInstance.id}")
    private Long jobInstanceId;

    private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;

    public OaiIdentifiersRepositoryItemReader(
        ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository) {
        this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setRepository(executionRecordExternalIdentifierRepository);
        setSort(Collections.emptyMap());
        setMethodName("findByIdentifier_ExecutionId");
        setArguments(List.of(jobInstanceId+""));
        Map<String, Direction> sorts = new HashMap<>();
        sorts.put("identifier.recordId", Direction.ASC);
        setSort(sorts);

        super.afterPropertiesSet();
    }
}
