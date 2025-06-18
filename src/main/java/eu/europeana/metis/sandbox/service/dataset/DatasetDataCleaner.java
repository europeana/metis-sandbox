package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for cleaning dataset entities.
 */
@Service
public class DatasetDataCleaner {

  private final DatasetRepository datasetRepository;

  /**
   * Constructor.
   *
   * @param datasetRepository repository responsible for performing dataset operations
   */
  public DatasetDataCleaner(DatasetRepository datasetRepository) {
    this.datasetRepository = datasetRepository;
  }

  /**
   * Removes a dataset entity by its identifier.
   *
   * <p>Attempts to delete the dataset associated with the provided identifier.
   *
   * @param datasetId identifier of the dataset to be removed
   * @throws {@link ServiceException} if an error occurs during removal.
   */
  public void remove(String datasetId) {
    try {
      datasetRepository.deleteById(Integer.valueOf(datasetId));
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error removing dataset id: [%s]. ", datasetId), e);
    }
  }
}
