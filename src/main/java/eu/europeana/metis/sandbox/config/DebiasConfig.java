package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.sandbox.common.debias.Event;
import eu.europeana.metis.sandbox.common.debias.State;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
@EnableStateMachine
public class DebiasConfig extends StateMachineConfigurerAdapter<State, Event> {

  @Override
  public void configure(StateMachineStateConfigurer<State, Event> states) throws Exception {
    states
        .withStates()
        .initial(State.START)
        .state(State.PROCESSING)
        .state(State.ERROR)
        .end(State.COMPLETED);
  }

  @Override
  public void configure(StateMachineTransitionConfigurer<State, Event> transitions) throws Exception {
    transitions
        .withExternal()
        .source(State.START).target(State.PROCESSING)
        .event(Event.PROCESS)
        .and()
        .withExternal()
        .source(State.PROCESSING).target(State.ERROR)
        .event(Event.FAIL)
        .and()
        .withExternal()
        .source(State.ERROR).target(State.PROCESSING)
        .event(Event.PROCESS)
        .and()
        .withExternal()
        .source(State.PROCESSING).target(State.COMPLETED)
        .event(Event.SUCCEED);
  }

  //  @Bean
  //  public StateMachineRuntimePersister<State,Event,String> stringStateMachineRuntimePersister() {
  //    return  new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);;
  //  }
  //  @Override
  //  public void configure(StateMachineConfigurationConfigurer<State, Event> config) throws Exception {
  //    config.withPersistence()
  //        .runtimePersister()
  //  }
}

