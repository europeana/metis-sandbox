package eu.europeana.metis.sandbox.config.web;

import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

  @Override
  public void addInterceptors(InterceptorRegistry registry){
    registry.addInterceptor(new RateLimitInterceptor(Integer.parseInt(capacity), Long.parseLong(time)))
            .addPathPatterns("/record/validation/**");
  }
}
