package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.common.config.properties.spring.actuator.InfoAppConfigurationProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({InfoAppConfigurationProperties.class})
class OpenApiConfig {

  /**
   * Creates and configures an OpenAPI object for API documentation using the provided configuration properties.
   *
   * @param infoAppConfigurationProperties the configuration properties containing details such as title, version, description,
   * and contact information for the API documentation
   * @return an OpenAPI instance configured with the provided details for documentation generation
   */
  @Bean
  OpenAPI openApi(InfoAppConfigurationProperties infoAppConfigurationProperties) {
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
