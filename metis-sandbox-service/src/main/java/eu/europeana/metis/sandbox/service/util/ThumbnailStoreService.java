package eu.europeana.metis.sandbox.service.util;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

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
import eu.europeana.metis.sandbox.common.S3Bucket;
import eu.europeana.metis.sandbox.entity.ThumbnailIdEntity;
import eu.europeana.metis.sandbox.repository.ThumbnailIdRepository;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling the storage and removal of thumbnail images in an Amazon S3 bucket and maintaining corresponding metadata
 * in a database.
 */
@Service
public class ThumbnailStoreService {

  private static final int BATCH_SIZE = 1000;

  private final AmazonS3 s3client;

  private final String thumbnailsS3BucketName;

  private final ThumbnailIdRepository thumbnailIdRepository;

  /**
   * Constructor.
   *
   * @param s3client the Amazon S3 client used for interacting with the S3 service
   * @param thumbnailsS3Bucket the S3 bucket where thumbnails are stored
   * @param thumbnailIdRepository the repository for storing and retrieving thumbnail metadata
   */
  public ThumbnailStoreService(AmazonS3 s3client, S3Bucket thumbnailsS3Bucket, ThumbnailIdRepository thumbnailIdRepository) {
    this.s3client = s3client;
    this.thumbnailsS3BucketName = requireNonNull(thumbnailsS3Bucket.name(), "Thumbnails bucket name must not be null");
    this.thumbnailIdRepository = thumbnailIdRepository;
  }

  /**
   * Stores a list of thumbnails in an S3 bucket and records their metadata in the database.
   *
   * <p>Ensures each thumbnail is stored in the S3 bucket, and metadata about the thumbnails is
   * saved into the database for tracking their association with the given dataset.
   * <p>Null thumbnails in the input list are ignored.
   *
   * @param thumbnails the list of thumbnails to be stored
   * @param datasetId the identifier of the dataset associated with the thumbnails
   * @throws NullPointerException if thumbnails or datasetId is null
   * @throws ThumbnailStoringException if a problem occurs during the storage of a thumbnail
   * @throws ServiceException if an error occurs while saving metadata to the database
   */
  public void store(List<Thumbnail> thumbnails, String datasetId) {
    requireNonNull(thumbnails, "Thumbnails must not be null");
    requireNonNull(datasetId, "Dataset id must not be null");

    // remove null objects
    List<Thumbnail> notNullThumbnails = thumbnails.stream().filter(Objects::nonNull).toList();

    // store each thumbnail in s3 bucket
    for (Thumbnail thumbnail : notNullThumbnails) {
      try (thumbnail) {
        store(thumbnail);
      } catch (IOException | SdkClientException e) {
        throw new ThumbnailStoringException(format("Issue processing thumbnail: [%s] ", thumbnail.getTargetName()), e);
      }
    }

    try {
      thumbnailIdRepository.saveAll(notNullThumbnails.stream()
                                                     .map(x -> new ThumbnailIdEntity(datasetId, x.getTargetName()))
                                                     .toList());
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error saving thumbnail entities: [%s] for dataset: [%s]. ",
          notNullThumbnails.stream().map(Thumbnail::getTargetName).collect(joining(",")),
          datasetId), e);
    }
  }

  /**
   * Removes thumbnails and associated data for the given dataset ID.
   *
   * <p>Deletes thumbnail entries in batches from external storage and also removes
   * the corresponding metadata from the database.
   *
   * @param datasetId the unique identifier of the dataset whose thumbnails need to be removed
   * @throws ServiceException if there is an error during the deletion process.
   */
  @Transactional
  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");

    // get thumbnails that belong to a given dataset
    List<ThumbnailIdEntity> thumbnailEntities = getThumbnailEntities(datasetId);

    // create objects for s3 batch deletes
    List<KeyVersion> thumbnailKeys = thumbnailEntities.stream()
                                                      .map(ThumbnailIdEntity::getThumbnailId)
                                                      .map(KeyVersion::new)
                                                      .toList();

    // split deletes in batches (s3 supports a max of 1000 per batch) and delete in batches
    Lists.partition(thumbnailKeys, BATCH_SIZE).forEach(this::deleteBatch);

    // remove thumbnail info in the DB
    try {
      thumbnailIdRepository.deleteByDatasetId(datasetId);
    } catch (RuntimeException e) {
      throw new ServiceException(
          format("Error deleting thumbnail entities for dataset: [%s]. ", datasetId), e);
    }
  }

  private void store(Thumbnail thumbnail) throws IOException {
    ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setContentLength(thumbnail.getContentSize());
    objectMetadata.setContentType(thumbnail.getMimeType());

    PutObjectRequest putObjectRequest = new PutObjectRequest(thumbnailsS3BucketName, thumbnail.getTargetName(),
        thumbnail.getContentStream(), objectMetadata);

    s3client.putObject(putObjectRequest);
  }

  private List<ThumbnailIdEntity> getThumbnailEntities(String datasetId) {
    try {
      return thumbnailIdRepository.findByDatasetId(datasetId);
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error getting thumbnails for dataset [%s]. ", datasetId),
          e);
    }
  }

  private void deleteBatch(List<KeyVersion> keyVersions) {
    DeleteObjectsRequest deleteObjectsRequest =
        new DeleteObjectsRequest(thumbnailsS3BucketName).withKeys(keyVersions).withQuiet(true);

    try {
      s3client.deleteObjects(deleteObjectsRequest);
    } catch (MultiObjectDeleteException e) {
      List<String> errors = e.getErrors().stream().map(DeleteError::getKey).toList();
      throw new ThumbnailRemoveException(errors, e);
    } catch (SdkClientException e) {
      throw new ThumbnailRemoveException(e);
    }
  }
}
