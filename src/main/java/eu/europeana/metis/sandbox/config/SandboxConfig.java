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
import eu.europeana.metis.sandbox.config.batch.WorkflowConfigurationProperties;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateService;
import eu.europeana.metis.utils.apm.ElasticAPMConfiguration;
import eu.europeana.normalization.NormalizerFactory;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration class for the Sandbox application.
 *
 * <p>This class sets up and provides general beans required for the operation of the sandbox.
 */
@Configuration
@ComponentScan("eu.europeana.validation.service")
@EnableConfigurationProperties({ElasticAPMConfiguration.class, WorkflowConfigurationProperties.class})
class SandboxConfig {

  private static final int WORKFLOW_CORE_POOL_SIZE = 4;
  private static final int WORKFLOW_QUEUE_CAPACITY = 20;

  @Value("${sandbox.enrichment.dereference-url}")
  private String dereferenceServiceUrl;

  @Value("${sandbox.enrichment.enrichment-properties.entity-management-url}")
  private String entityManagementUrl;

  @Value("${sandbox.enrichment.enrichment-properties.entity-api-url}")
  private String entityApiUrl;

  @Value("${sandbox.enrichment.enrichment-properties.entity-api-token-endpoint}")
  private String entityApiTokenEndpoint;

  @Value("${sandbox.enrichment.enrichment-properties.entity-api-grant-params}")
  private String entityApiGrantParams;

  @Value("${sandbox.portal.publish.record-base-url}")
  private String portalPublishRecordBaseUrl;

  @Bean(name = "portalPublishRecordBaseUrl")
  String portalPublishRecordBaseUrl() {
    return portalPublishRecordBaseUrl;
  }

  @Bean(name = "pipelineTaskExecutor")
  TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(WORKFLOW_CORE_POOL_SIZE);
    executor.setMaxPoolSize(WORKFLOW_CORE_POOL_SIZE);
    executor.setQueueCapacity(WORKFLOW_QUEUE_CAPACITY);
    executor.setThreadNamePrefix("PipelineJob-");
    executor.initialize();
    return executor;
  }

  @Bean
  public UrlValidator urlValidator() {
    return new UrlValidator(new String[]{"http", "https"});
  }

  @Bean
  XsltUrlUpdateService xsltUrlUpdateService(TransformXsltRepository transformXsltRepository,
      LockRegistry lockRegistry, HttpClient httpClient) {
    return new XsltUrlUpdateService(transformXsltRepository, lockRegistry, httpClient);
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
  NormalizerFactory normalizerFactory() {
    return new NormalizerFactory();
  }

  @Bean
  EnrichmentWorker enrichmentWorker() throws DereferenceException, EnrichmentException {
    DereferencerProvider dereferencerProvider = new DereferencerProvider();
    dereferencerProvider.setDereferenceUrl(dereferenceServiceUrl);
    dereferencerProvider.setEnrichmentPropertiesValues(entityManagementUrl, entityApiUrl, entityApiTokenEndpoint,
        entityApiGrantParams);
    EnricherProvider enricherProvider = new EnricherProvider();
    enricherProvider.setEnrichmentPropertiesValues(entityManagementUrl, entityApiUrl, entityApiTokenEndpoint,
        entityApiGrantParams);
    return new EnrichmentWorkerImpl(dereferencerProvider.create(), enricherProvider.create());
  }

  @Bean
  HttpClient httpClient() {
    return HttpClient.newBuilder().version(Version.HTTP_2)
                     .followRedirects(Redirect.NORMAL)
                     .connectTimeout(Duration.ofSeconds(5))
                     .build();
  }
}
