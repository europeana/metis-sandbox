package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRecordExternalIdentifierRepository extends JpaRepository<ExecutionRecordExternalIdentifier, ExecutionRecordIdentifier> {

    Page<ExecutionRecordExternalIdentifier> findByIdentifier_ExecutionId(String executionId, Pageable pageable);
    long countByIdentifier_DatasetIdAndIdentifier_ExecutionId(String datasetId, String executionId);

}
