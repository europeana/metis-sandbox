package eu.europeana.metis.sandbox.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;

public abstract class TestContainerIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestContainerIT.class);
  abstract void logConfiguration();
  abstract void dynamicProperties(DynamicPropertyRegistry registry);

}
