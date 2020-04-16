package eu.europeana.metis.sandbox.service.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.sandbox.common.exception.ThumbnailProcessingException;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class ThumbnailServiceImpl implements ThumbnailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailServiceImpl.class);

  @Override
  public void storeThumbnails(List<Thumbnail> thumbnails) {
    for (Thumbnail thumbnail : thumbnails) {
      LOGGER.info("Thumbnail {} Mime-Type {}",
          thumbnail.getTargetName(), thumbnail.getMimeType());
      try {
        storeThumbnail(thumbnail);
        thumbnail.close();
      } catch (IOException e) {
        throw new ThumbnailProcessingException(thumbnail.getTargetName(), e);
      }
    }
  }

  private void storeThumbnail(Thumbnail thumbnail) throws IOException {
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(Regions.DEFAULT_REGION)
        .build();

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(thumbnail.getContentSize());
    metadata.setContentType(thumbnail.getMimeType());

    PutObjectRequest request = new PutObjectRequest("bucketName", thumbnail.getTargetName(),
        thumbnail.getContentStream(), metadata);

    s3Client.putObject(request);
  }
}
