package eu.europeana.metis.sandbox.service.problempatterns;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExecutionPointServiceTest {

    @Mock
    private ExecutionPointRepository executionPointRepository;

    @InjectMocks
    private ExecutionPointService executionPointService;

    @Test
    void getAllExecutionTimestamps_expectSuccess(){
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
    void getExecutionPoint_expectSuccess(){
        executionPointService.getExecutionPoint("1", Step.VALIDATE_INTERNAL.toString());
        verify(executionPointRepository, times(1))
                .findFirstByDatasetIdAndExecutionStepOrderByExecutionTimestamp("1", Step.VALIDATE_INTERNAL.toString());

    }
}
