package eu.europeana.metis.sandbox.config;

import static java.util.stream.Collectors.joining;

import co.elastic.apm.attach.ElasticApmAttacher;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "elastic")
public class ElasticConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(ElasticConfig.class);
  private static final String NEW_LINE_TAB = "\n\t";
  private final Map<String, String> apmProperties = new HashMap<>();

  /**
   * The name of this method has to be as the key in the application.yml
   * elastic:
   *  apm:
   *    ...
   *
   * @return map of elastic apm agent properties
   */
  public Map<String, String> getApm() {
    return this.apmProperties;
  }

  @Override
  public String toString() {
    return NEW_LINE_TAB + apmProperties.entrySet().stream().map(Objects::toString)
                                       .collect(joining(NEW_LINE_TAB));
  }

  @PostConstruct
  public void attachApmAgent() {
    LOGGER.debug("{}", this);
    ElasticApmAttacher.attach(this.apmProperties);
  }
}
