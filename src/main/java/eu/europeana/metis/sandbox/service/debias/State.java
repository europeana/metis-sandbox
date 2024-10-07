package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;

/**
 * The type State.
 */
public abstract class State {

  /**
   * The State machine.
   */
  protected DeBiasStateful stateMachine;
  /**
   * The Detect repository.
   */
  protected DatasetDeBiasRepository datasetDeBiasRepository;
  /**
   * The Name.
   */
  protected String name;
  /**
   * The Terminal state.
   */
  protected boolean terminalState;
}
