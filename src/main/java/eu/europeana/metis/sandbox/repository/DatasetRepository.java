package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.projection.DatasetIdView;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
  List<DatasetIdView> getByCreatedDateBefore(LocalDateTime date);

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
