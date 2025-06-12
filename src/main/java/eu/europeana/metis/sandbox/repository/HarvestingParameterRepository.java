package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository connected to Harvesting Parameters table
 */
public interface HarvestingParameterRepository extends JpaRepository<HarvestParametersEntity, UUID> {

    @Transactional(readOnly = true)
    Optional<HarvestParametersEntity> findById(@NotNull UUID id);

    @Transactional(readOnly = true)
    Optional<HarvestParametersEntity> findByDatasetEntity_DatasetId(Integer datasetId);

    /**
     * Removes the entity from the table based on the dataset id
     * @param datasetId The dataset id associated to the entity
     */
    @Modifying
    @Query("DELETE FROM HarvestParametersEntity hpe WHERE hpe.datasetEntity.datasetId = :datasetId")
    void deleteByDatasetIdDatasetId(@Param("datasetId") Integer datasetId);
}
