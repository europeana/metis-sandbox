package eu.europeana.metis.sandbox.test.utils;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

public class S3ContainerIT extends TestContainer {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3ContainerIT.class);
  private static LocalStackContainer s3Container;
  private static AmazonS3 s3Client;
  public static final String S3_VERSION = "localstack/localstack:1.1.0";
  public static final String BUCKET_NAME = "test-thumbnails-bucket";

  public S3ContainerIT() {
    s3Container = new LocalStackContainer(DockerImageName.parse(S3_VERSION))
        .withServices(S3);
    s3Container.start();

    s3Client = AmazonS3ClientBuilder
        .standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(s3Container.getEndpointOverride(S3).toString(),
                s3Container.getRegion()))
        .build();
    s3Client.createBucket(BUCKET_NAME);

    logConfiguration();
  }

  @Override
  public void logConfiguration() {
    LOGGER.info("S3 service container created:");
    if (!s3Container.getAccessKey().isBlank() && !s3Container.getSecretKey().isBlank()) {
      LOGGER.info("Access key and Secret key were loaded");
    }
    LOGGER.info("Endpoint: {}", s3Container.getEndpointOverride(S3));
    LOGGER.info("Signing Region: {}", s3Container.getRegion());
  }

  @Override
  public void dynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.aws.accessKeyId", s3Container::getAccessKey);
    registry.add("spring.aws.secretKey", s3Container::getSecretKey);
    registry.add("spring.s3.endpoint", () -> s3Container.getEndpointOverride(S3));
    registry.add("spring.s3.region", s3Container::getRegion);
    registry.add("spring.s3.bucket", () -> BUCKET_NAME);

    // TODO: 13/09/2022 We should perhaps remove the specifics here and use the default spring configuration properties
    //Sandbox specific datasource properties
    registry.add("sandbox.s3.access-key", s3Container::getAccessKey);
    registry.add("sandbox.s3.secret-key", s3Container::getSecretKey);
    registry.add("sandbox.s3.endpoint", () -> s3Container.getEndpointOverride(S3));
    registry.add("sandbox.s3.signing-region", s3Container::getRegion);
    registry.add("sandbox.s3.thumbnails-bucket", () -> BUCKET_NAME);
  }

  @Override
  public void runScripts(List<String> scripts) {
    //nothing to do at this moment
  }
}
