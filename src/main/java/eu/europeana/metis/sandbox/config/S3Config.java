package eu.europeana.metis.sandbox.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import eu.europeana.metis.sandbox.domain.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Config for S3 bucket
 */
@Configuration
class S3Config {

  @Value("${sandbox.s3.access-key}")
  private String accessKey;

  @Value("${sandbox.s3.secret-key}")
  private String secretKey;

  @Value("${sandbox.s3.endpoint}")
  private String endpoint;

  @Value("${sandbox.s3.signing-region}")
  private String signingRegion;

  @Value("${sandbox.s3.thumbnails-bucket}")
  private String thumbnailsBucket;

  @Bean
  @ConditionalOnProperty(
      name="sandbox.s3.local-enabled",
      havingValue = "true")
  AmazonS3 s3ClientHttp() {
    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    ClientConfiguration clientConfig = new ClientConfiguration();
    clientConfig.setProtocol(Protocol.HTTP);
    return AmazonS3ClientBuilder
        .standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withPathStyleAccessEnabled(true)
        .withClientConfiguration(clientConfig)
        .withEndpointConfiguration(
            new EndpointConfiguration(endpoint, signingRegion))
        .build();
  }

  @Bean
  @ConditionalOnProperty(
      name="sandbox.s3.local-enabled",
      havingValue = "false",
      matchIfMissing = true)
  AmazonS3 s3ClientHttps() {
    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    return AmazonS3ClientBuilder
        .standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(
            new EndpointConfiguration(endpoint, signingRegion))
        .build();
  }

  @Bean
  Bucket thumbnailsBucket() {
    return new Bucket(thumbnailsBucket);
  }
}
