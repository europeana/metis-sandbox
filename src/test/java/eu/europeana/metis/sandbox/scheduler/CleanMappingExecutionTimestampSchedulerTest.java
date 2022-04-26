package eu.europeana.metis.sandbox.scheduler;

import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CleanMappingExecutionTimestampSchedulerTest {

    @Mock
    private InternalValidationService internalValidationService;

    @InjectMocks
    private CleanMappingExecutionTimestampScheduler scheduler;

    @Test
    void cleanMapping_expectSuccess(){
        scheduler.cleanMapping();
        verify(internalValidationService, times(1)).cleanMappingExecutionTimestamp();
    }
}
