package eu.europeana.metis.sandbox.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.DatasetWithExecutionProgressSummaryDTO;
import eu.europeana.metis.sandbox.dto.harvest.FileHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
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

    List<DatasetWithExecutionProgressSummaryDTO> result = userService.getDatasetInfoWithProgressOwnedByUserId(USER_ID);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(datasetReportService, times(1)).getDatasetInfoByUserId(USER_ID);
    verify(datasetReportService, never()).getProgress(anyString());
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

    ExecutionProgressInfoDTO executionProgressInfoDTO1 = mock(ExecutionProgressInfoDTO.class);
    ExecutionProgressInfoDTO executionProgressInfoDTO2 = mock(ExecutionProgressInfoDTO.class);
    when(datasetReportService.getProgress("dataset1")).thenReturn(executionProgressInfoDTO1);
    when(datasetReportService.getProgress("dataset2")).thenReturn(executionProgressInfoDTO2);

    List<DatasetWithExecutionProgressSummaryDTO> result = userService.getDatasetInfoWithProgressOwnedByUserId(USER_ID);

    assertNotNull(result);

    assertEquals(2, result.size());

    DatasetWithExecutionProgressSummaryDTO datasetWithExecutionProgressInfoDTO1 = result.get(0);
    DatasetWithExecutionProgressSummaryDTO datasetWithExecutionProgressInfoDTO2 = result.get(1);

    assertEquals(datasetInfoDTO1.getDatasetId(), datasetWithExecutionProgressInfoDTO1.datasetId());
    assertEquals(executionProgressInfoDTO1.executionStatus(), datasetWithExecutionProgressInfoDTO1.executionStatus());
    assertEquals(datasetInfoDTO2.getDatasetId(), datasetWithExecutionProgressInfoDTO2.datasetId());
    assertEquals(executionProgressInfoDTO2.executionStatus(), datasetWithExecutionProgressInfoDTO2.executionStatus());

    verify(datasetReportService, times(1)).getDatasetInfoByUserId(USER_ID);
    verify(datasetReportService, times(1)).getProgress("dataset1");
    verify(datasetReportService, times(1)).getProgress("dataset2");
    verifyNoMoreInteractions(datasetReportService);
  }
}
