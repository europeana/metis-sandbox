package eu.europeana.metis.sandbox.service.util;

import static java.util.Objects.requireNonNull;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import eu.europeana.metis.sandbox.domain.Bucket;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class ThumbnailStoreServiceImpl implements ThumbnailStoreService {

  private final AmazonS3 s3client;

  private final Bucket thumbnailsBucket;

  public ThumbnailStoreServiceImpl(AmazonS3 s3client,
      Bucket thumbnailsBucket) {
    this.s3client = s3client;
    this.thumbnailsBucket = thumbnailsBucket;
  }

  @Override
  public void store(List<Thumbnail> thumbnails) {
    requireNonNull(thumbnails, "Thumbnails must not be null");
    var notNullThumbnails = thumbnails.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    for (Thumbnail thumbnail : notNullThumbnails) {
      try (thumbnail) {
        store(thumbnail);
      } catch (IOException | SdkClientException e) {
        throw new ThumbnailStoringException(thumbnail.getTargetName(), e);
      }
    }
  }

  private void store(Thumbnail thumbnail) throws IOException {
    var metadata = new ObjectMetadata();
    metadata.setContentLength(thumbnail.getContentSize());
    metadata.setContentType(thumbnail.getMimeType());

    var request = new PutObjectRequest(
        thumbnailsBucket.getName(), thumbnail.getTargetName(),
        thumbnail.getContentStream(), metadata);

    s3client.putObject(request);
  }
}
