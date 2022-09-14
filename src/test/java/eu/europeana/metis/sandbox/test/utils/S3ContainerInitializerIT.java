package eu.europeana.metis.sandbox.test.utils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

public class S3ContainerInitializerIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ContainerInitializerIT.class);
    static final LocalStackContainer s3Container;
    static final AmazonS3 s3Client;
    public static final String S3_VERSION = "localstack/localstack:1.1.0";
    public static final String BUCKET_NAME = "test-thumbnails-bucket";

    static {
        s3Container = new LocalStackContainer(DockerImageName.parse(S3_VERSION))
                .withServices(S3);
        s3Container.start();

        s3Client = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(s3Container.getEndpointConfiguration(S3))
                .withCredentials(s3Container.getDefaultCredentialsProvider())
                .build();
        s3Client.createBucket(BUCKET_NAME);

        logConfiguration();
    }

    private static void logConfiguration() {
        LOGGER.info("S3 service container created:");
        LOGGER.info("Access Key Id: {}", s3Container.getAccessKey());
        LOGGER.info("Secret Key: {}", s3Container.getSecretKey());
        LOGGER.info("Endpoint: {}", s3Container.getEndpointConfiguration(S3).getServiceEndpoint());
        LOGGER.info("Signing Region: {}", s3Container.getEndpointConfiguration(S3).getSigningRegion());
    }

    public static void dynamicProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.aws.accessKeyId", s3Container::getAccessKey);
        registry.add("spring.aws.secretKey", s3Container::getSecretKey);
        registry.add("spring.s3.endpoint", () -> s3Container.getEndpointConfiguration(S3).getServiceEndpoint());
        registry.add("spring.s3.region", s3Container::getRegion);
        registry.add("spring.s3.bucket", () -> BUCKET_NAME);

        // TODO: 13/09/2022 We should perhaps remove the specifics here and use the default spring configuration properties
        //Sandbox specific datasource properties
        registry.add("sandbox.s3.access-key", s3Container::getAccessKey);
        registry.add("sandbox.s3.secret-key", s3Container::getSecretKey);
        registry.add("sandbox.s3.endpoint", () -> s3Container.getEndpointConfiguration(S3).getServiceEndpoint());
        registry.add("sandbox.s3.signing-region", s3Container::getRegion);
        registry.add("sandbox.s3.thumbnails-bucket", () -> BUCKET_NAME);


    }

    private void close() {
        //We do not close the container, Ryuk will handle closing at the end of all the unit tests.
    }
}
