package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.metis.utils.ZipFileReader;
import eu.europeana.validation.service.ClasspathResourceResolver;
import eu.europeana.validation.service.PredefinedSchemasGenerator;
import eu.europeana.validation.service.SchemaProvider;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.xml.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ComponentScan("eu.europeana.validation.service")
public class SandboxConfig {

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String initialQueue;

  @Value("${sandbox.dataset.creation.threads.core-pool-size}")
  private Integer corePoolSize;

  @Value("${sandbox.dataset.creation.threads.max-pool-size}")
  private Integer maxPoolSize;

  @Value("${sandbox.dataset.creation.threads.thread-prefix}")
  private String threadPrefix;

  private String edmSorterUrl = null;

  private final ResourceLoader resourceLoader;

  public SandboxConfig(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Bean
  @Scope("prototype")
  XPathFactory xPathFactory() {
    return XPathFactory.newDefaultInstance();
  }

  @Bean
  @Scope("prototype")
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
  SchemaProvider schemaProvider() throws IOException {
    return new SchemaProvider(PredefinedSchemasGenerator.generate(schemaProperties()));
  }

  private Properties schemaProperties() throws IOException {
    PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
    propertiesFactoryBean.setLocation(new ClassPathResource("predefined-schemas.properties"));
    propertiesFactoryBean.afterPropertiesSet();
    return propertiesFactoryBean.getObject();
  }
}
