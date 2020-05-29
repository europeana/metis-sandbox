package eu.europeana.metis.sandbox.service.util;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.MultiObjectDeleteException.DeleteError;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.collect.Lists;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.exception.ThumbnailRemoveException;
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import eu.europeana.metis.sandbox.domain.Bucket;
import eu.europeana.metis.sandbox.entity.ThumbnailEntity;
import eu.europeana.metis.sandbox.repository.ThumbnailRepository;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ThumbnailStoreServiceImpl implements ThumbnailStoreService {

  private static final int BATCH_SIZE = 1000;

  private final AmazonS3 s3client;

  private final Bucket thumbnailsBucket;

  private final ThumbnailRepository thumbnailRepository;

  public ThumbnailStoreServiceImpl(AmazonS3 s3client,
      Bucket thumbnailsBucket,
      ThumbnailRepository thumbnailRepository) {
    this.s3client = s3client;
    this.thumbnailsBucket = thumbnailsBucket;
    this.thumbnailRepository = thumbnailRepository;
  }

  @Override
  public void store(List<Thumbnail> thumbnails, String datasetId) {
    requireNonNull(thumbnails, "Thumbnails must not be null");
    requireNonNull(datasetId, "Dataset id must not be null");

    // remove null objects
    var notNullThumbnails = thumbnails.stream()
        .filter(Objects::nonNull)
        .collect(toList());

    // store each thumbnail in s3 bucket
    for (Thumbnail thumbnail : notNullThumbnails) {
      try (thumbnail) {
        store(thumbnail);
      } catch (IOException | SdkClientException e) {
        throw new ThumbnailStoringException(thumbnail.getTargetName(), e);
      }
    }

    // store thumbnail info in DB to keep track of bucket contents
    try {
      thumbnailRepository.saveAll(notNullThumbnails.stream()
          .map(x -> new ThumbnailEntity(datasetId, x.getTargetName()))
          .collect(toList()));
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error saving thumbnail entities: [%s] for dataset: [%s]. ",
          notNullThumbnails.stream().map(Thumbnail::getTargetName).collect(joining(",")),
          datasetId), e);
    }
  }

  @Override
  @Transactional
  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");

    // get thumbnails that belong to given dataset
    var thumbnailEntities = getThumbnailEntities(datasetId);

    // create objects for s3 batch deletes
    var thumbnailKeys = thumbnailEntities.stream()
        .map(ThumbnailEntity::getThumbnailId)
        .map(KeyVersion::new)
        .collect(toList());

    // split deletes in batches (s3 supports a max of 1000 per batch) and delete in batches
    Lists.partition(thumbnailKeys, BATCH_SIZE).forEach(this::deleteBatch);

    // remove thumbnail info in the DB
    try {
      thumbnailRepository.deleteByDatasetId(datasetId);
    } catch (RuntimeException e) {
      throw new ServiceException(
          format("Error deleting thumbnail entities for dataset: [%s]. ", datasetId), e);
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

  private List<ThumbnailEntity> getThumbnailEntities(String datasetId) {
    try {
      return thumbnailRepository.findByDatasetId(datasetId);
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error getting thumbnails for dataset [%s]. ", datasetId),
          e);
    }
  }

  private void deleteBatch(List<KeyVersion> list) {
    var request = new DeleteObjectsRequest(thumbnailsBucket.getName())
        .withKeys(list).withQuiet(true);

    try {
      s3client.deleteObjects(request);
    } catch (MultiObjectDeleteException e) {
      var errors = e.getErrors().stream()
          .map(DeleteError::getKey)
          .collect(toList());
      throw new ThumbnailRemoveException(errors, e);
    } catch (SdkClientException e) {
      throw new ThumbnailRemoveException(e);
    }
  }
}
