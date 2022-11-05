package eu.europeana.metis.sandbox.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;

public abstract class TestContainer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestContainer.class);
  abstract void logConfiguration();
  abstract void dynamicProperties(DynamicPropertyRegistry registry);

}
