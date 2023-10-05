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
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateService;
import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateServiceImpl;
import eu.europeana.metis.sandbox.service.validationworkflow.ExternalValidationStep;
import eu.europeana.metis.sandbox.service.validationworkflow.HarvestValidationStep;
import eu.europeana.metis.sandbox.service.validationworkflow.InternalValidationValidationStep;
import eu.europeana.metis.sandbox.service.validationworkflow.TransformationValidationStep;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationStep;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.validation.service.ClasspathResourceResolver;
import eu.europeana.validation.service.PredefinedSchemasGenerator;
import eu.europeana.validation.service.SchemaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.xml.xpath.XPathFactory;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

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

    @Value("${sandbox.enrichment.enrichment-properties.entity-management-url}")
    private String entityManagementUrl;

    @Value("${sandbox.enrichment.enrichment-properties.entity-api-url}")
    private String entityApiUrl;

    @Value("${sandbox.enrichment.enrichment-properties.entity-api-key}")
    private String entityApiKey;

    //TODO: 04-03-2021 We should remove this configuration once
    //TODO: XsltTransformation allows local files. Ticket MET-3450 was created to fix this issue
    @Value("${sandbox.validation.edm-sorter-url}")
    private String edmSorterUrl;

    @Value("${sandbox.transformation.xslt-url}")
    private String defaultXsltUrl;

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

    @Bean(name = "createdQueue")
    String createdQueue() {
        return createdQueue;
    }

    @Bean(name = "transformationToEdmExternalQueue")
    String transformationToEdmExternalQueue() {
        return transformationToEdmExternalQueue;
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
        dereferencerProvider.setEnrichmentPropertiesValues(entityManagementUrl, entityApiUrl, entityApiKey);
        EnricherProvider enricherProvider = new EnricherProvider();
        enricherProvider.setEnrichmentPropertiesValues(entityManagementUrl, entityApiUrl, entityApiKey);
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

    @Bean
    HttpClient httpClient() {
        return HttpClient.newBuilder().version(Version.HTTP_2)
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    ValidationStep harvestValidationStep(RecordLogService recordLogService) {
        return new HarvestValidationStep(recordLogService);
    }

    @Bean
    ValidationStep externalValidationStep(ExternalValidationService externalValidationService,
                                          RecordLogService recordLogService) {
        return new ExternalValidationStep(externalValidationService, recordLogService);
    }

    @Bean
    ValidationStep transformationValidationStep(TransformationService transformationService,
                                                RecordLogService recordLogService) {
        return new TransformationValidationStep(transformationService, recordLogService);
    }

    @Bean
    ValidationStep internalValidationValidationStep(InternalValidationService internalValidationService,
                                                    RecordLogService recordLogService) {
        return new InternalValidationValidationStep(internalValidationService, recordLogService);
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
