package eu.europeana.metis.sandbox.config;

import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;

@Configuration
public class LockRepositoryJdbcConfig {

  @Value("${spring.integration.defaultlockrepository.prefix:integration.int_}")
  private String prefix;
  @Value("${spring.integration.defaultlockrepository.ttl:120000}")
  private int timeToLive;
  @Value("${spring.integration.defaultlockrepository.region:DEFAULT}")
  private String region;

  @Bean
  public DefaultLockRepository defaultLockRepository(DataSource dataSource) {
    DefaultLockRepository defaultLockRepository = new DefaultLockRepository(dataSource);
    defaultLockRepository.setPrefix(prefix);
    defaultLockRepository.setTimeToLive(timeToLive);
    defaultLockRepository.setRegion(region);
    return defaultLockRepository;
  }

  @Bean
  public JdbcLockRegistry jdbcLockRegistry(LockRepository repository) {
    JdbcLockRegistry jdbcLockRegistry = new JdbcLockRegistry(repository);
    jdbcLockRegistry.setIdleBetweenTries(Duration.ofSeconds(1));
    return jdbcLockRegistry;
  }
}
