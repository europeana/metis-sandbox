package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type State.
 */
public abstract class State {

  /**
   * The State machine.
   */
  protected DetectService stateMachine;
  /**
   * The Detect repository.
   */
  protected DetectRepository detectRepository;
  /**
   * The Name.
   */
  protected String name;
  /**
   * The Terminal state.
   */
  protected boolean terminalState;
}
