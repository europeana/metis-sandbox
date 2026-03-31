package eu.europeana.metis.sandbox.integration.testcontainers;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
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
  private static final String S3MOCK_IMAGE = "adobe/s3mock:latest";
  private static final GenericContainer<?> s3MockContainer;
  private static final String INITIAL_BUCKETS_ENV_KEY = "initialBuckets";
  public static final String BUCKET_NAME = "test-thumbnails-bucket";
  public static final int S3MOCK_PORT = 9090;
  public static final String ACCESS_KEY = "test";
  public static final String SECRET_KEY = "test";
  public static final String REGION = "eu";


  static {
    s3MockContainer =
        new GenericContainer<>(DockerImageName.parse(S3MOCK_IMAGE))
            .withEnv(INITIAL_BUCKETS_ENV_KEY, BUCKET_NAME)
            .withExposedPorts(S3MOCK_PORT)
            .waitingFor(Wait.forListeningPort());

    s3MockContainer.start();
    setDynamicProperties();
    logConfiguration();
  }

  public static String getEndpoint() {
    return String.format("http://%s:%s", s3MockContainer.getHost(), s3MockContainer.getMappedPort(S3MOCK_PORT));
  }

  private static void setDynamicProperties() {
    System.setProperty("spring.aws.accessKeyId", ACCESS_KEY);
    System.setProperty("spring.aws.secretKey", SECRET_KEY);
    System.setProperty("spring.s3.endpoint", getEndpoint());
    System.setProperty("spring.s3.region", REGION);
    System.setProperty("spring.s3.bucket", BUCKET_NAME);
  }

  public static void setDynamicProperty(String key, Function<GenericContainer<?>, String> getValue) {
    System.setProperty(key, getValue.apply(s3MockContainer));
  }

  private static void logConfiguration() {
    LOGGER.info("S3Mock container created:");
    LOGGER.info("Endpoint: {}", getEndpoint());
    LOGGER.info("Region: {}", REGION);
    LOGGER.info("Bucket: {}", BUCKET_NAME);
  }
}
