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
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

  private static final String EXCEPTION_MODEL = "ExceptionModel";

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
    return List.of(new ResponseMessageBuilder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("500 Internal Error")
            .responseModel(new ModelRef(EXCEPTION_MODEL))
            .build(),
        new ResponseMessageBuilder()
            .code(HttpStatus.BAD_REQUEST.value())
            .message("400 Bad Request")
            .responseModel(new ModelRef(EXCEPTION_MODEL))
            .build());
  }

  private List<ResponseMessage> getExceptionModelList() {
    return List.of(new ResponseMessageBuilder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("500 Internal Error")
            .responseModel(new ModelRef(EXCEPTION_MODEL))
            .build(),
        new ResponseMessageBuilder()
            .code(HttpStatus.NOT_FOUND.value())
            .message("404 Not Found")
            .responseModel(new ModelRef(EXCEPTION_MODEL))
            .build(),
        new ResponseMessageBuilder()
            .code(HttpStatus.BAD_REQUEST.value())
            .message("400 Bad Request")
            .responseModel(new ModelRef(EXCEPTION_MODEL))
            .build());
  }
}
