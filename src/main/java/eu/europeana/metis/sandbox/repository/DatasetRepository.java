package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.projection.DatasetIdView;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Integer> {

  /**
   * Get a list of datasets created before specified date
   *
   * @param date must not be null
   * @return list of dataset ids
   * @see DatasetIdView
   * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation">Query Creation</a>
   * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#projections">Projections</a>
   */
  List<DatasetIdView> getByCreatedDateBefore(ZonedDateTime date);

  /**
   * Updates the value of recordQuantity to the given dataset
   * @param datasetId The id of the dataset to update to
   * @param quantity The new value to update into the dataset
   */
  @Modifying
  @Query("UPDATE DatasetEntity dataset SET dataset.recordsQuantity = ?2 WHERE dataset.datasetId = ?1")
  void updateRecordsQuantity(int datasetId, Long quantity);

  /**
   * Sets to true the boolean recordLimitExceeded
   * @param datasetId The id of the dataset to update this into
   */
  @Modifying
  @Query("UPDATE DatasetEntity dataset SET dataset.recordLimitExceeded = true WHERE dataset.datasetId = ?1")
  void setRecordLimitExceeded(int datasetId);

  /**
   * A boolean type of query to check if dataset has xslt content
   * @param datasetId The id of the dataset to update into
   * @return Returns 0 if there is no xslt, 1 otherwise
   */
  @Query("SELECT COUNT(*) FROM DatasetEntity dataset WHERE dataset.datasetId = ?1 AND dataset.xsltEdmExternalContent IS NOT NULL")
  int isXsltPresent(int datasetId);

  /**
   * Get xslt content based on datasetId
   *
   * @param datasetId must not be null
   * @return xslt content associated to dataset
   */
  @Query("SELECT " +
      "dataset.xsltEdmExternalContent " +
      "FROM " +
      "    DatasetEntity dataset " +
      "WHERE dataset.datasetId = ?1 ")
  String getXsltContentFromDatasetId(int datasetId);

}
