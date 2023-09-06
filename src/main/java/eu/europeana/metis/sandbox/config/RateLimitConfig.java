package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfigurationBuilder;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.postgresql.PostgreSQLadvisoryLockBasedProxyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class RateLimitConfig {

    @Value("${sandbox.rate-limit.bandwidth.capacity}")
    private String capacity;

    @Value("${sandbox.rate-limit.bandwidth.time}")
    private String time;

    @Bean
    RateLimitInterceptor rateLimitInterceptor(DataSource dataSource){
        SQLProxyConfiguration<Long> sqlProxyConfiguration = SQLProxyConfigurationBuilder.builder()
                .withClientSideConfig(ClientSideConfig.getDefault().withClientClock(TimeMeter.SYSTEM_MILLISECONDS))
                .withTableSettings(BucketTableSettings.customSettings("rate_limit.buckets", "id", "bucket_state"))
                .build(dataSource);
        PostgreSQLadvisoryLockBasedProxyManager proxyManager = new PostgreSQLadvisoryLockBasedProxyManager(sqlProxyConfiguration);
        return new RateLimitInterceptor(Integer.parseInt(capacity), Long.parseLong(time), proxyManager);

    }
}
