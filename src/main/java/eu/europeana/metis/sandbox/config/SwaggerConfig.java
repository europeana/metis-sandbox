package eu.europeana.metis.sandbox.config;

import com.fasterxml.classmate.TypeResolver;
import eu.europeana.metis.sandbox.dto.ExceptionModelDto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Config for Swagger documentation
 */
@Configuration
@EnableSwagger2
class SwaggerConfig {

  private static final String MESSAGE_FOR_400_CODE = ""
          + "<span style=\"font-style: normal; font-size: 125%; font-weight: 750;\">"
          + "400 Bad Request</span>"
          + " (or any other 4xx or 5xx error status code)"
          + " - The response body will contain an object of type"
          + " <span style=\"font-style: normal; font-size: 125%; font-weight: 750;\">"
          + ExceptionModelDto.SWAGGER_MODEL_NAME + "</span>.";

  @Value("${info.app.title}")
  private String title;

  @Value("${info.app.version}")
  private String version;

  @Value("${info.app.description}")
  private String description;

  private final TypeResolver typeResolver;

  public SwaggerConfig(TypeResolver typeResolver) {
    this.typeResolver = typeResolver;
  }

  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.basePackage("eu.europeana.metis.sandbox"))
        .paths(PathSelectors.any())
        .build()
        .apiInfo(apiInfo())
        .useDefaultResponseMessages(false)
        // The line allows swagger annotations to set Object.class to suppress the result type.
        .directModelSubstitute(Object.class, Void.class)
        .additionalModels(typeResolver.resolve(ExceptionModelDto.class))
        .globalResponseMessage(RequestMethod.POST,
            postExceptionModelList())
        .globalResponseMessage(RequestMethod.GET,
            getExceptionModelList());
  }

  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title(title)
        .description(description)
        .version(version)
        .build();
  }

  private List<ResponseMessage> postExceptionModelList() {
    return List.of(
        new ResponseMessageBuilder()
            .code(HttpStatus.BAD_REQUEST.value())
            .message(MESSAGE_FOR_400_CODE)
            .build());
  }

  private List<ResponseMessage> getExceptionModelList() {
    return List.of(
        new ResponseMessageBuilder()
            .code(HttpStatus.BAD_REQUEST.value())
            .message(MESSAGE_FOR_400_CODE)
            .build());
  }
}
