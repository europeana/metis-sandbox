package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.validation.service.ClasspathResourceResolver;
import eu.europeana.validation.service.PredefinedSchemasGenerator;
import eu.europeana.validation.service.SchemaProvider;
import eu.europeana.validation.service.ValidationServiceConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
class ValidateConfig {

  @Value("${sandbox.validation.executor.pool-size}")
  private Integer threadCount;

  //TODO: 04-03-2021 We should remove this configuration once
  //TODO: XsltTransformation allows local files. Ticket MET-3450 was created to fix this issue
  @Value("${sandbox.validation.edm-sorter-url}")
  private String edmSorterUrl;

  @Bean
  @ConfigurationProperties(prefix = "sandbox.validation")
  Schema schema() {
    return new Schema();
  }

  @Bean
  ClasspathResourceResolver lsResourceResolver() {
    return new ClasspathResourceResolver();
  }

  @Bean
  SchemaProvider schemaProvider() {
    return new SchemaProvider(PredefinedSchemasGenerator.generate(schemaProperties()));
  }

  @Bean
  ValidationServiceConfig validationServiceConfig() {
    return () -> threadCount;
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  XsltTransformer xsltEdmSorter() throws TransformationException {
    return new XsltTransformer(edmSorterUrl);
  }

  private Properties schemaProperties() {
    Schema schema = schema();
    Map<String, Object> predefinedSchemas = schema.getPredefinedSchemas();
    Properties schemaProps = new Properties();
    schemaProps.put(Schema.PREDEFINED_SCHEMAS, String.join(",", predefinedSchemas.keySet()));
    addSchemaProperties(Schema.PREDEFINED_SCHEMAS, predefinedSchemas, schemaProps);
    return schemaProps;
  }

  private void addSchemaProperties(String key, Object value, Properties props) {
    switch (value) {
      case String string -> props.setProperty(key, string);
      case Map<?, ?> map -> {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          String nestedKey = key + "." + entry.getKey().toString();
          Object nestedValue = entry.getValue();
          addSchemaProperties(nestedKey, nestedValue, props);
        }
      }
      case null, default -> throw new IllegalArgumentException("Property value: " + value);
    }
  }

  @Getter
  private static class Schema {

    public static final String PREDEFINED_SCHEMAS = "predefinedSchemas";
    private final Map<String, Object> predefinedSchemas = new HashMap<>();
  }
}
