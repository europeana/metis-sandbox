package eu.europeana.metis.sandbox.service.user;

import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.DatasetSummaryDTO;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Service class for managing user information.
 */
@Service
public class UserService {

  private final DatasetReportService datasetReportService;

  /**
   * Constructor.
   *
   * @param datasetReportService the DatasetReportService used to retrieve dataset information and execution progress details.
   */
  public UserService(DatasetReportService datasetReportService) {
    this.datasetReportService = datasetReportService;
  }

  /**
   * Retrieves a list of datasets owned by a specific user along with their execution progress information.
   *
   * @param userId the unique identifier of the user whose datasets are to be retrieved
   * @return a list of {@link DatasetSummaryDTO} objects combining dataset metadata and corresponding
   * execution progress information
   */
  public List<DatasetSummaryDTO> getDatasetInfoWithProgressOwnedByUserId(String userId) {
    List<DatasetSummaryDTO> datasetSummaryDTOS = new ArrayList<>();
    List<DatasetInfoDTO> datasetInfoByUserId = datasetReportService.getDatasetInfoByUserId(userId);
    for (DatasetInfoDTO datasetInfoDTO : datasetInfoByUserId) {
      DatasetSummaryDTO datasetSummaryDTO =
          DatasetSummaryDTO.from(datasetInfoDTO);
      datasetSummaryDTOS.add(datasetSummaryDTO);
    }
    return datasetSummaryDTOS;
  }
}
