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
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateService;
import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateServiceImpl;
import eu.europeana.metis.utils.apm.ElasticAPMConfiguration;
import eu.europeana.normalization.NormalizerFactory;
import java.lang.invoke.MethodHandles;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.concurrent.Executor;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ComponentScan("eu.europeana.validation.service")
@EnableConfigurationProperties({ElasticAPMConfiguration.class})
class SandboxConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("${sandbox.dataset.creation.threads.core-pool-size}")
    private Integer corePoolSize;

    @Value("${sandbox.dataset.creation.threads.max-pool-size}")
    private Integer maxPoolSize;

    @Value("${sandbox.dataset.creation.threads.thread-prefix}")
    private String threadPrefix;

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

    @Bean(name = "pipelineTaskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("PipelineJob-");
        executor.initialize();
        return executor;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    XPathFactory xPathFactory() {
        return XPathFactory.newDefaultInstance();
    }

    @Bean
    XsltUrlUpdateService xsltUrlUpdateService(TransformXsltRepository transformXsltRepository,
                                              LockRegistry lockRegistry, HttpClient httpClient) {
        return new XsltUrlUpdateServiceImpl(transformXsltRepository, lockRegistry, httpClient);
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
    NormalizerFactory normalizerFactory() {
        return new NormalizerFactory();
    }

    @Bean
    EnrichmentWorker enrichmentWorker() throws DereferenceException, EnrichmentException {
        DereferencerProvider dereferencerProvider = new DereferencerProvider();
        dereferencerProvider.setDereferenceUrl(dereferenceServiceUrl);
        dereferencerProvider.setEnrichmentPropertiesValues(entityManagementUrl, entityApiUrl, entityApiTokenEndpoint, entityApiGrantParams);
        EnricherProvider enricherProvider = new EnricherProvider();
        enricherProvider.setEnrichmentPropertiesValues(entityManagementUrl, entityApiUrl, entityApiTokenEndpoint, entityApiGrantParams);
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
    HttpClient httpClient() {
        return HttpClient.newBuilder().version(Version.HTTP_2)
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }


}
