package eu.europeana.metis.sandbox.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.DatasetSummaryDTO;
import eu.europeana.metis.sandbox.dto.harvest.FileHarvestParametersDTO;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final String USER_ID = "UserId";

  @Mock
  private DatasetReportService datasetReportService;

  @InjectMocks
  private UserService userService;

  @Test
  void returnsEmptyListWhenNoDatasets() {
    when(datasetReportService.getDatasetInfoByUserId(USER_ID)).thenReturn(List.of());

    List<DatasetSummaryDTO> result = userService.getDatasetInfoWithProgressOwnedByUserId(USER_ID);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(datasetReportService, times(1)).getDatasetInfoByUserId(USER_ID);
    verifyNoMoreInteractions(datasetReportService);
  }


  @Test
  void returnsListWithProgressForEachDataset() {

    DatasetInfoDTO datasetInfoDTO1 = mock(DatasetInfoDTO.class);
    DatasetInfoDTO datasetInfoDTO2 = mock(DatasetInfoDTO.class);
    when(datasetInfoDTO1.getDatasetId()).thenReturn("dataset1");
    when(datasetInfoDTO1.getAbstractHarvestParametersDTO()).thenReturn(
        new FileHarvestParametersDTO("fileName", FileType.ZIP, new byte[0],1));
    when(datasetInfoDTO2.getDatasetId()).thenReturn("dataset2");
    when(datasetInfoDTO2.getAbstractHarvestParametersDTO()).thenReturn(
        new FileHarvestParametersDTO("fileName", FileType.ZIP, new byte[0],1));

    List<DatasetInfoDTO> datasetInfoDTOs = List.of(datasetInfoDTO1, datasetInfoDTO2);
    when(datasetReportService.getDatasetInfoByUserId(USER_ID)).thenReturn(datasetInfoDTOs);

    List<DatasetSummaryDTO> result = userService.getDatasetInfoWithProgressOwnedByUserId(USER_ID);

    assertNotNull(result);

    assertEquals(2, result.size());

    DatasetSummaryDTO datasetSummaryDTO1 = result.get(0);
    DatasetSummaryDTO datasetSummaryDTO2 = result.get(1);

    assertEquals(datasetInfoDTO1.getDatasetId(), datasetSummaryDTO1.datasetId());
    assertEquals(datasetInfoDTO2.getDatasetId(), datasetSummaryDTO2.datasetId());

    verify(datasetReportService, times(1)).getDatasetInfoByUserId(USER_ID);
    verifyNoMoreInteractions(datasetReportService);
  }
}
