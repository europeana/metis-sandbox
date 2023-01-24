package eu.europeana.metis.sandbox.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;

@Configuration
public class ConcurrencyJdbcConfig {

  @Bean
  public DefaultLockRepository defaultLockRepository(DataSource dataSource) {
    DefaultLockRepository defaultLockRepository = new DefaultLockRepository(dataSource);
    defaultLockRepository.setPrefix("integration.int_");
    return defaultLockRepository;
  }

  @Bean
  public JdbcLockRegistry jdbcLockRegistry(LockRepository repository) {
    return new JdbcLockRegistry(repository);
  }
}
