package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.HarvestingParametersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HarvestingParametersRepository extends JpaRepository<HarvestingParametersEntity, Long> {
    HarvestingParametersEntity getHarvestingParametersEntitiesByDatasetId_DatasetId(Integer datasetId);

    void deleteByDatasetId_DatasetId(Integer datasetId);
}
