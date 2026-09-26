package eu.europeana.metis.sandbox.common.task.input;

import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains the information required to create a task in the sandbox processing engine.
 */
@Getter
@Setter
@NoArgsConstructor
public class SandboxTaskRequest {

  private Map<SandboxTaskKey, String> parameters;
  private InputMetadataRequest inputMetadataRequest;
}
