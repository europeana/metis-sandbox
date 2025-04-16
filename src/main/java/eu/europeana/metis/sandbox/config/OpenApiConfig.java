package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.common.config.properties.spring.actuator.InfoAppConfigurationProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Config for Open API documentation
 */
@Configuration
@EnableConfigurationProperties({InfoAppConfigurationProperties.class})
public class OpenApiConfig {

  @Bean
  public OpenAPI openApi(InfoAppConfigurationProperties infoAppConfigurationProperties) {
    final Contact contact = new Contact();
    contact.email(infoAppConfigurationProperties.contact().email());
    contact.name(infoAppConfigurationProperties.contact().name());
    contact.url(infoAppConfigurationProperties.contact().url());

    final Info info = new Info()
        .title(infoAppConfigurationProperties.title())
        .description(infoAppConfigurationProperties.description())
        .version(infoAppConfigurationProperties.version())
        .contact(contact);

    return new OpenAPI().info(info);
  }
}
