package eu.europeana.metis.sandbox.common.task.input;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a task that can be submitted to a processing engine.
 */
@Getter
@Setter
public class SandboxTask {
  private Map<SandboxTaskKey, String> parameters;
  private InputMetadataRequest inputMetadataRequest;
}
