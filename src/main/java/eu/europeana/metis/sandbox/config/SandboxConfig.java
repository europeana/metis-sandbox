package eu.europeana.metis.sandbox.config;

import eu.europeana.enrichment.api.internal.EntityResolver;
import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.EnrichmentWorkerImpl;
import eu.europeana.enrichment.rest.client.dereference.DereferenceClient;
import eu.europeana.enrichment.rest.client.dereference.Dereferencer;
import eu.europeana.enrichment.rest.client.dereference.DereferencerImpl;
import eu.europeana.enrichment.rest.client.enrichment.Enricher;
import eu.europeana.enrichment.rest.client.enrichment.EnricherImpl;
import eu.europeana.enrichment.rest.client.enrichment.MetisRecordParser;
import eu.europeana.enrichment.rest.client.enrichment.RemoteEntityResolver;
import eu.europeana.enrichment.rest.client.exceptions.EnrichmentException;
import eu.europeana.enrichment.utils.EntityMergeEngine;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.RdfConverterFactory;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.metis.utils.ZipFileReader;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.validation.service.ClasspathResourceResolver;
import eu.europeana.validation.service.PredefinedSchemasGenerator;
import eu.europeana.validation.service.SchemaProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.xml.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
@ComponentScan("eu.europeana.validation.service")
class SandboxConfig {

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String initialQueue;

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

  private String defaultXsltUrl;

  private String edmSorterUrl;

  private final ResourceLoader resourceLoader;

  public SandboxConfig(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  XPathFactory xPathFactory() {
    return XPathFactory.newDefaultInstance();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  XsltTransformer xsltEdmSorter() throws IOException, TransformationException {
    return new XsltTransformer(edmSorterUrl());
  }

  private String edmSorterUrl() throws IOException {
    if (edmSorterUrl == null) {
      edmSorterUrl = resourceLoader.getResource("classpath:edm/edm.xsd.sorter.xsl").getURL()
          .toString();
    }
    return edmSorterUrl;
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  XsltTransformer xsltTransformer(String datasetName, String edmCountry, String edmLanguage)
      throws TransformationException {
    return new XsltTransformer(defaultXsltUrl, datasetName, edmCountry, edmLanguage);
  }

  @Bean
  Executor asyncDatasetPublishServiceTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setThreadNamePrefix(threadPrefix);
    executor.initialize();
    return executor;
  }

  @Bean
  String initialQueue() {
    return initialQueue;
  }

  @Bean
  ZipFileReader zipFileReader() {
    return new ZipFileReader();
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
  EnrichmentWorker enrichmentWorker() throws EnrichmentException {

    DereferenceClient dereferenceClient = new DereferenceClient(new RestTemplate(), dereferenceServiceUrl);
    EntityResolver entityResolver;

    try {
      entityResolver = new RemoteEntityResolver(new URL(enrichmentServiceUrl), 10, new RestTemplate()); //TODO: Config batch size
    } catch (MalformedURLException e) {
        throw new EnrichmentException("There was trouble setting up the enrichmentServiceUrl", e);
    }

    Dereferencer dereferencer = new DereferencerImpl(new EntityMergeEngine(), entityResolver, dereferenceClient);
    Enricher enricher = new EnricherImpl(new MetisRecordParser(), entityResolver, new EntityMergeEngine());
    return new EnrichmentWorkerImpl(dereferencer, enricher);

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
