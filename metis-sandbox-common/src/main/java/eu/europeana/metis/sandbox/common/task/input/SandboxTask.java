package eu.europeana.metis.sandbox.common.task.input;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a task created in the sandbox processing engine.
 */
@Getter
@Setter
@NoArgsConstructor
public class SandboxTask extends SandboxTaskRequest {

  private String taskId;
  private String batchId;

  /**
   * Creates a task from its request and engine identifiers.
   *
   * @param sandboxTaskRequest the task creation request
   * @param taskId the task identifier
  * @param batchId the batch identifier
  */
  public SandboxTask(SandboxTaskRequest sandboxTaskRequest, String taskId, String batchId) {
    super(sandboxTaskRequest);
    this.taskId = taskId;
    this.batchId = batchId;
  }
}
