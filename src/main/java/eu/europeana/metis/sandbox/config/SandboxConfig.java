package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.utils.ZipFileReader;
import java.util.concurrent.Executor;
import javax.xml.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SandboxConfig {

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String initialQueue;

  @Value("${sandbox.dataset.creation.threads.core-pool-size}")
  private Integer corePoolSize;

  @Value("${sandbox.dataset.creation.threads.max-pool-size}")
  private Integer maxPoolSize;

  @Value("${sandbox.dataset.creation.threads.thread-prefix}")
  private String threadPrefix;

  @Bean
  @Scope("prototype")
  XPathFactory xPathFactory() {
    return XPathFactory.newDefaultInstance();
  }

  @Bean
  Executor taskExecutor() {
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
}
