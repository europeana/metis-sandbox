package eu.europeana.metis.sandbox.config;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Config for Open API documentation
 */
@Configuration
class OpenApiConfiguration {

    @Value("${info.app.title}")
    private String title;

    @Value("${info.app.version}")
    private String version;

    @Value("${info.app.description}")
    private String description;

    @Value("${info.app.contact.name}")
    private String contactName;

    @Value("${info.app.contact.email}")
    private String contactEmail;

    @Value("${info.app.contact.url}")
    private String contactUrl;

    @Bean
    public GroupedOpenApi sandboxApi() {
        return GroupedOpenApi.builder()
                .group("metis-sandbox")
                .packagesToScan("eu.europeana.metis.sandbox")
                .pathsToMatch("/dataset/**", "/pattern-analysis/**", "/record/**")
                .addOpenApiCustomiser(openApi -> openApi.info(apiInfo()))
                .build();
    }

    private Info apiInfo() {
        Contact contact = new Contact();
        contact.email(contactEmail);
        contact.name(contactName);
        contact.url(contactUrl);

        return new Info()
                .title(title)
                .description(description)
                .version(version)
                .contact(contact);
    }
}
