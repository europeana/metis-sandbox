package eu.europeana.metis.sandbox.config;

import static java.util.Objects.requireNonNull;

import co.elastic.apm.attach.ElasticApmAttacher;
import eu.europeana.metis.sandbox.common.exception.ElasticConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

public class ElasticConfig {

  private static final String ELASTIC_KEY_ROOT = "elastic.apm.";
  private static String configurationResource = "application.yml";

  private ElasticConfig() {
    //Not to be instantiated
  }

  public static void setConfigurationResource(String configurationResource) {
    ElasticConfig.configurationResource = configurationResource;
  }

  public static Map<String, String> loadAndGetConfig() {
    Map<String, String> properties;
    try {
      YamlPropertySourceLoader propertySourceLoader = new YamlPropertySourceLoader();
      Resource resource = new InputStreamResource(requireNonNull(ElasticConfig.class
          .getClassLoader()
          .getResourceAsStream(configurationResource)));
      List<PropertySource<?>> propertySourceList = propertySourceLoader.load(configurationResource, resource);
      Map<String, Object> temp = new HashMap<>();
      for (PropertySource<?> source : propertySourceList) {
        temp.putAll(((MapPropertySource) source).getSource());
      }
      properties = temp.entrySet().stream()
                       .filter(x -> x.getKey().contains(ELASTIC_KEY_ROOT))
                       .collect(Collectors
                           .toMap(x -> x.getKey().replace(ELASTIC_KEY_ROOT, ""),
                               x -> String.valueOf(x.getValue())));
    } catch (Exception e) {
      throw new ElasticConfigurationException("Properties configuration", e);
    }
    return properties;
  }

  public static boolean loadAttacher() {
    try {
      ElasticApmAttacher.attach(ElasticConfig.loadAndGetConfig());
      return true;
    } catch (ElasticConfigurationException configurationException) {
      Map<String, String> disabledConfig = new HashMap<>();
      disabledConfig.put("enabled", "false");
      ElasticApmAttacher.attach(disabledConfig);
      return false;
    }
  }
}
