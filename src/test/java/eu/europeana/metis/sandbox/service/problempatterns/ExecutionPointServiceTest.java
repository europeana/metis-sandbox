package eu.europeana.metis.sandbox.service.problempatterns;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExecutionPointServiceTest {

  @Mock
  private ExecutionPointRepository executionPointRepository;

  @InjectMocks
  private ExecutionPointService executionPointService;

  @Test
  void getAllExecutionTimestamps_expectSuccess() {
    List<ExecutionPoint> mockList = new ArrayList<>();
    ExecutionPoint executionPoint1 = new ExecutionPoint();
    LocalDateTime executionPoint1Timestamp = LocalDateTime.now();
    executionPoint1.setExecutionTimestamp(executionPoint1Timestamp);
    ExecutionPoint executionPoint2 = new ExecutionPoint();
    LocalDateTime executionPoint2Timestamp = LocalDateTime.now();
    executionPoint2.setExecutionTimestamp(executionPoint2Timestamp);
    mockList.add(executionPoint1);
    mockList.add(executionPoint2);
    when(executionPointRepository.findAll()).thenReturn(mockList);

    Set<LocalDateTime> result = executionPointService.getAllExecutionTimestamps();
    assertTrue(result.contains(executionPoint1Timestamp));
    assertTrue(result.contains(executionPoint2Timestamp));

  }

  @Test
  void getExecutionPoint_expectSuccess() {
    executionPointService.getExecutionPoint("1", FullBatchJobType.VALIDATE_INTERNAL.name());
    verify(executionPointRepository, times(1))
        .findFirstByDatasetIdAndExecutionNameOrderByExecutionTimestampDesc("1", FullBatchJobType.VALIDATE_INTERNAL.name());

  }
}
