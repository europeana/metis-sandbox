package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordExternalIdentifierRepository extends JpaRepository<ExecutionRecordExternalIdentifier, ExecutionRecordIdentifierKey> {

    Page<ExecutionRecordExternalIdentifier> findByIdentifier_ExecutionId(String executionId, Pageable pageable);

    void removeByIdentifier_DatasetId(String datasetId);
    //    long countByIdentifier_DatasetIdAndIdentifier_ExecutionId(String datasetId, String executionId);

}
