package eu.europeana.metis.sandbox.service.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExecutionPointService {

    private final ExecutionPointRepository executionPointRepository;

    public ExecutionPointService(ExecutionPointRepository executionPointRepository){
        this.executionPointRepository = executionPointRepository;
    }


    /**
     * Method that retrieves all possible execution timestamps available
     * @return A set of unique timestamps saved in the database
     */
    public Set<LocalDateTime> getAllExecutionTimestamps(){
        return executionPointRepository.findAll().stream().map(ExecutionPoint::getExecutionTimestamp).collect(Collectors.toUnmodifiableSet());
    }
}
