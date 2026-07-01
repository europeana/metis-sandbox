package eu.europeana.metis.sandbox.service.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for handling operations related to execution points.
 */
@Service
public class ExecutionPointService {

  private final ExecutionPointRepository executionPointRepository;

  /**
   * Constructor.
   *
   * @param executionPointRepository The repository used for managing execution point entities.
   */
  public ExecutionPointService(ExecutionPointRepository executionPointRepository) {
    this.executionPointRepository = executionPointRepository;
  }


  /**
   * Method that retrieves all possible execution timestamps available
   *
   * @return A set of unique timestamps saved in the database
   */
  public Set<Instant> getAllExecutionTimestamps() {
    return executionPointRepository.findAll().stream().map(ExecutionPoint::getExecutionTimestamp)
                                   .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Retrieves an execution point associated with dataset id and its execution step.
   * <p>
   * The sorting is descending so that we get the latest execution.
   * </p>
   *
   * @param datasetId The dataset id
   * @param executionStep The execution step as a string
   * @return An optional object wrapping the execution point
   */
  public Optional<ExecutionPoint> getLatestExecutionPoint(String datasetId, String executionStep) {
    return executionPointRepository.findFirstByDatasetIdAndExecutionNameOrderByExecutionTimestampDesc(datasetId, executionStep);
  }

  /**
   * Retrieves an execution point by its unique identifier.
   *
   * @param executionPointId The unique identifier of the execution point to retrieve.
   * @return An {@code Optional} containing the execution point if found, or an empty {@code Optional} if not found.
   */
  public Optional<ExecutionPoint> getExecutionPoint(Integer executionPointId) {
    return executionPointRepository.findById(executionPointId);
  }
}
