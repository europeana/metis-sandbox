package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class State {

  public static final Logger LOGGER = LoggerFactory.getLogger(State.class);

  protected DetectService stateMachine;
  protected DetectRepository detectRepository;
  protected String name;
  protected boolean terminalState;
}
