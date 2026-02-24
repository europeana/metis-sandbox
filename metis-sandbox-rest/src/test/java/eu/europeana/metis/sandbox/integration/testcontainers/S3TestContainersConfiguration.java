package eu.europeana.metis.sandbox.integration.testcontainers;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides {@link TestConfiguration} S3(LocalStack) Testcontainers.
 * <p>
 * This class it meant to be executed during integration tests which would initialize a single static containers to be used for
 * multiple tests. To use this, {@link Import} it in test classes.
 * <p>
 * Notice: do not change the static nature of the components unless there is an explicit requirement for a container per test.
 */
@TestConfiguration
public class S3TestContainersConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String S3_VERSION = "localstack/localstack:s3-latest";
  private static final LocalStackContainer s3Container;
  private static final AmazonS3 s3Client;
  public static final String BUCKET_NAME = "test-thumbnails-bucket";

  static {
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
    setDynamicProperties();
    logConfiguration();
  }

  private static void setDynamicProperties() {
    System.setProperty("spring.aws.accessKeyId", s3Container.getAccessKey());
    System.setProperty("spring.aws.secretKey", s3Container.getSecretKey());
    System.setProperty("spring.s3.endpoint", s3Container.getEndpointOverride(S3).toString());
    System.setProperty("spring.s3.region", s3Container.getRegion());
    System.setProperty("spring.s3.bucket", BUCKET_NAME);
  }

  public static void setDynamicProperty(String key, Function<LocalStackContainer, String> getValue) {
    System.setProperty(key, getValue.apply(s3Container));
  }

  private static void logConfiguration() {
    LOGGER.info("S3 service container created:");
    if (!s3Container.getAccessKey().isBlank() && !s3Container.getSecretKey().isBlank()) {
      LOGGER.info("Access key and Secret key were loaded");
    }
    LOGGER.info("Endpoint: {}", s3Container.getEndpointOverride(S3));
    LOGGER.info("Signing Region: {}", s3Container.getRegion());
  }
}
