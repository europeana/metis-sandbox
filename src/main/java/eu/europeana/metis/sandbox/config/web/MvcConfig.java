package eu.europeana.metis.sandbox.config.web;

import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfigurationBuilder;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.postgresql.PostgreSQLadvisoryLockBasedProxyManager;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

/**
 * MVC configuration. Binds Country and Language converters to the {@link FormatterRegistry}. Also
 * contains CORS configuration.
 */
@Configuration
class MvcConfig implements WebMvcConfigurer {

  @Value("${sandbox.cors.mapping}")
  private String[] corsMapping;

  @Value("${sandbox.rate-limit.bandwidth.capacity}")
  private String capacity;

  @Value("${sandbox.rate-limit.bandwidth.time}")
  private String time;

  private RateLimitInterceptor rateLimitInterceptor;

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "swagger-ui.html");
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringToCountryConverter());
    registry.addConverter(new StringToLanguageConverter());
    registry.addConverter(new StringToTimestampConverter());
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    if (ArrayUtils.isNotEmpty(corsMapping)) {
      registry.addMapping("/**").allowedOrigins(corsMapping)
          .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS");
    }
  }
  @Bean
  RateLimitInterceptor rateLimitInterceptor(DataSource dataSource){
    SQLProxyConfiguration<Long> sqlProxyConfiguration = SQLProxyConfigurationBuilder.builder()
            .withClientSideConfig(ClientSideConfig.getDefault().withClientClock(TimeMeter.SYSTEM_MILLISECONDS))
            .withTableSettings(BucketTableSettings.customSettings("rate_limit.buckets", "id", "bucket_state"))
            .build(dataSource);
    PostgreSQLadvisoryLockBasedProxyManager proxyManager = new PostgreSQLadvisoryLockBasedProxyManager(sqlProxyConfiguration);
    rateLimitInterceptor = new RateLimitInterceptor(Integer.parseInt(capacity), Long.parseLong(time), proxyManager);
    return rateLimitInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry){
    registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/record/validation/**");
  }
}
