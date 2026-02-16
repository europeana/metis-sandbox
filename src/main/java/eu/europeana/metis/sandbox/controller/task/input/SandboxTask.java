package eu.europeana.metis.sandbox.controller.task.input;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a task that can be submitted to a processing engine.
 */
@Getter
@Setter
//todo:decide if we keep this class as is, it was more strict before use in controller.
public class SandboxTask {
  private Map<SandboxTaskKey, String> parameters;
  private InputMetadataRequest inputMetadataRequest;
}
