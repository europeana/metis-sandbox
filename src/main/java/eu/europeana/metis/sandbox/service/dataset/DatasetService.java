package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository.DatasetIdProjection;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DatasetService {

  private final DatasetRepository datasetRepository;

  public DatasetService(DatasetRepository datasetRepository) {
    this.datasetRepository = datasetRepository;
  }

  public void setRecordLimitExceeded(String datasetId) {
    datasetRepository.setRecordLimitExceeded(Integer.parseInt(datasetId));
  }

  public void remove(String datasetId) {
    try {
      datasetRepository.deleteById(Integer.valueOf(datasetId));
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error removing dataset id: [%s]. ", datasetId), e);
    }
  }

  public List<String> findDatasetIdsByCreatedBefore(int days) {
    ZonedDateTime retentionDate = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(days);

    try {
      return datasetRepository.findByCreatedDateBefore(retentionDate).stream()
                              .map(DatasetIdProjection::getDatasetId)
                              .map(Object::toString)
                              .toList();
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error getting datasets older than %s days. ", days), e);
    }
  }
}
