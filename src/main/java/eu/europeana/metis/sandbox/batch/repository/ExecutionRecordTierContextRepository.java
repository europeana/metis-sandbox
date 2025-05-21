package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordTierContextRepository extends
    JpaRepository<ExecutionRecordTierContext, ExecutionRecordIdentifier> {

  List<ExecutionRecordTierContext> findByIdentifier_DatasetId(String datasetId);

  List<ExecutionRecordTierContext> findTop10ByIdentifier_DatasetIdAndContentTier(String datasetId, String contentTier);

  List<ExecutionRecordTierContext> findTop10ByIdentifier_DatasetIdAndMetadataTier(String datasetId, String metadataTier);

  long countByIdentifier_DatasetIdAndContentTier(String datasetId, String contentTier);

  long countByIdentifier_DatasetIdAndMetadataTier(String datasetId, String metadataTier);
}

