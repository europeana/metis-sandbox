package eu.europeana.metis.sandbox.config;

import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.EnrichmentWorkerImpl;
import eu.europeana.enrichment.rest.client.dereference.DereferencerProvider;
import eu.europeana.enrichment.rest.client.enrichment.EnricherProvider;
import eu.europeana.enrichment.rest.client.exceptions.DereferenceException;
import eu.europeana.enrichment.rest.client.exceptions.EnrichmentException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.mediaprocessing.MediaExtractor;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.RdfConverterFactory;
import eu.europeana.metis.mediaprocessing.RdfDeserializer;
import eu.europeana.metis.mediaprocessing.RdfSerializer;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.validation.service.ClasspathResourceResolver;
import eu.europeana.validation.service.PredefinedSchemasGenerator;
import eu.europeana.validation.service.SchemaProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
@ComponentScan("eu.europeana.validation.service")
class SandboxConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(SandboxConfig.class);

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String createdQueue;

  @Value("${sandbox.rabbitmq.queues.record.transformation.edm.external.queue}")
  private String transformationToEdmExternalQueue;

  @Value("${sandbox.dataset.creation.threads.core-pool-size}")
  private Integer corePoolSize;

  @Value("${sandbox.dataset.creation.threads.max-pool-size}")
  private Integer maxPoolSize;

  @Value("${sandbox.dataset.creation.threads.thread-prefix}")
  private String threadPrefix;

  @Value("${sandbox.enrichment.dereference-url}")
  private String dereferenceServiceUrl;

  @Value("${sandbox.enrichment.enrichment-url}")
  private String enrichmentServiceUrl;

  //TODO: 04-03-2021 We should remove this configuration once
  //TODO: XsltTransformation allows local files. Ticket MET-3450 was created to fix this issue
  @Value("${sandbox.validation.edm-sorter-url}")
  private String edmSorterUrl;

  private String defaultXsltUrl;

  @Value("${sandbox.dataset.provider-record-url-template}")
  private String providerRecordUrlTemplate;

  @Value("${sandbox.portal.publish.record-base-url}")
  private String portalPublishRecordBaseUrl;

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  XPathFactory xPathFactory() {
    return XPathFactory.newDefaultInstance();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  XsltTransformer xsltEdmSorter() throws TransformationException {
    return new XsltTransformer(edmSorterUrl);
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  XsltTransformer xsltTransformer(String datasetName, String edmCountry, String edmLanguage)
      throws TransformationException {
    return new XsltTransformer(defaultXsltUrl, datasetName, edmCountry, edmLanguage);
  }

  @Bean
  Executor asyncServiceTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setThreadNamePrefix(threadPrefix);
    executor.initialize();
    return executor;
  }

  @Bean(name = "createdQueue")
  String createdQueue() {
    return createdQueue;
  }

  @Bean(name = "transformationToEdmExternalQueue")
  String transformationToEdmExternalQueue() {
    return transformationToEdmExternalQueue;
  }

  @Bean(name = "providerRecordUrlTemplate")
  String providerRecordUrlTemplate() {
    return providerRecordUrlTemplate;
  }

  @Bean(name = "portalPublishRecordBaseUrl")
  String portalPublishRecordBaseUrl() {
    return portalPublishRecordBaseUrl;
  }

  @Bean
  OaiHarvester oaiHarvester() {
    return HarvesterFactory.createOaiHarvester();
  }

  @Bean
  HttpHarvester httpHarvester() {
    return HarvesterFactory.createHttpHarvester();
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
  NormalizerFactory normalizerFactory() {
    return new NormalizerFactory();
  }

  @Bean
  EnrichmentWorker enrichmentWorker() throws DereferenceException, EnrichmentException {
    DereferencerProvider dereferencerProvider = new DereferencerProvider();
    dereferencerProvider.setDereferenceUrl(dereferenceServiceUrl);
    dereferencerProvider.setEnrichmentUrl(enrichmentServiceUrl);
    EnricherProvider enricherProvider = new EnricherProvider();
    enricherProvider.setEnrichmentUrl(enrichmentServiceUrl);
    return new EnrichmentWorkerImpl(dereferencerProvider.create(), enricherProvider.create());
  }

  @Bean
  RdfConverterFactory rdfConverterFactory() {
    return new RdfConverterFactory();
  }

  @Bean
  MediaProcessorFactory mediaProcessorFactory() {
    return new MediaProcessorFactory();
  }

  @Bean
  MediaExtractor mediaExtractor() {
    try {
      return mediaProcessorFactory().createMediaExtractor();
    } catch (MediaProcessorException mediaProcessorException) {
      LOGGER.error("Unable to create media extractor", mediaProcessorException);
      return null;
    }
  }

  @Bean
  RdfSerializer rdfSerializer() {
    return rdfConverterFactory().createRdfSerializer();
  }

  @Bean
  RdfDeserializer rdfDeserializer() {
    return rdfConverterFactory().createRdfDeserializer();
  }

  @Bean
  @ConfigurationProperties(prefix = "sandbox.validation")
  Schema schema() {
    return new Schema();
  }

  @Value("${sandbox.transformation.xslt-url}")
  void setDefaultXsltUrl(String defaultXsltUrl) {
    if (defaultXsltUrl == null || defaultXsltUrl.isEmpty()) {
      throw new IllegalArgumentException("defaultXsltUrl not provided");
    }
    this.defaultXsltUrl = defaultXsltUrl;
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
    if (value instanceof String) {
      props.setProperty(key, (String) value);
    } else if (value instanceof Map) {
      var map = ((Map<?, ?>) value);
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        addSchemaProperties(key + "." + entry.getKey(), entry.getValue(), props);
      }
    } else {
      throw new IllegalArgumentException("Property value: " + value);
    }
  }

  private static class Schema {

    public static final String PREDEFINED_SCHEMAS = "predefinedSchemas";

    private final Map<String, Object> predefinedSchemas = new HashMap<>();

    public Map<String, Object> getPredefinedSchemas() {
      return this.predefinedSchemas;
    }
  }
}
