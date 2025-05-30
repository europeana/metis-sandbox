package eu.europeana.metis.sandbox.service.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.PutObjectRequest;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.exception.ThumbnailRemoveException;
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import eu.europeana.metis.sandbox.domain.Bucket;
import eu.europeana.metis.sandbox.entity.ThumbnailIdEntity;
import eu.europeana.metis.sandbox.repository.ThumbnailIdRepository;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThumbnailStoreServiceTest {

  @Mock
  private AmazonS3 s3client;

  @Mock
  private ThumbnailIdRepository thumbnailIdRepository;

  private ThumbnailStoreService service;

  @BeforeEach
  public void setup() {
    service = new ThumbnailStoreService(s3client, new Bucket("bucket"), thumbnailIdRepository);
  }

  @Test
  void store_expectSuccess() {
    var thumbnail1 = mock(Thumbnail.class);
    var thumbnail2 = mock(Thumbnail.class);

    when(thumbnail1.getMimeType()).thenReturn("image/jpg");
    when(thumbnail1.getTargetName()).thenReturn("image1");
    when(thumbnail2.getMimeType()).thenReturn("image/jpg");
    when(thumbnail2.getTargetName()).thenReturn("image2");

    service.store(List.of(thumbnail1, thumbnail2), "1");

    verify(s3client, times(2)).putObject(any(PutObjectRequest.class));
    verify(thumbnailIdRepository).saveAll(anyList());
  }

  @Test
  void store_storingThumbnail_expectFail() {
    var thumbnail1 = mock(Thumbnail.class);
    var thumbnail2 = mock(Thumbnail.class);

    when(thumbnail1.getMimeType()).thenReturn("image/jpg");
    when(thumbnail1.getTargetName()).thenReturn("image1");

    when(s3client.putObject(any(PutObjectRequest.class))).thenThrow(new SdkClientException(""));

    assertThrows(ThumbnailStoringException.class,
        () -> service.store(List.of(thumbnail1, thumbnail2), "1"));

    verify(s3client).putObject(any(PutObjectRequest.class));
    verifyNoMoreInteractions(s3client);
    verifyNoInteractions(thumbnailIdRepository);
  }

  @Test
  void store_closingThumbnailException_expectFail() throws IOException {
    var thumbnail1 = mock(Thumbnail.class);
    var thumbnail2 = mock(Thumbnail.class);

    when(thumbnail1.getMimeType()).thenReturn("image/jpg");
    when(thumbnail1.getTargetName()).thenReturn("image1");
    doThrow(new ThumbnailStoringException("", new Exception())).when(thumbnail1)
        .close();

    when(s3client.putObject(any(PutObjectRequest.class))).thenThrow(new SdkClientException(""));

    assertThrows(ThumbnailStoringException.class,
        () -> service.store(List.of(thumbnail1, thumbnail2), "1"));

    verify(s3client).putObject(any(PutObjectRequest.class));
    verifyNoMoreInteractions(s3client);
    verifyNoInteractions(thumbnailIdRepository);
  }

  @Test
  void store_saveThumbnailToDB_expectFail() {
    var thumbnail1 = mock(Thumbnail.class);
    var thumbnail2 = mock(Thumbnail.class);

    when(thumbnail1.getMimeType()).thenReturn("image/jpg");
    when(thumbnail1.getTargetName()).thenReturn("image1");
    when(thumbnail2.getMimeType()).thenReturn("image/jpg");
    when(thumbnail2.getTargetName()).thenReturn("image2");

    when(thumbnailIdRepository.saveAll(anyList()))
        .thenThrow(new RuntimeException("Fail", new Exception()));

    assertThrows(ServiceException.class,
        () -> service.store(List.of(thumbnail1, thumbnail2), "1"));

    verify(s3client, times(2)).putObject(any(PutObjectRequest.class));
    verify(thumbnailIdRepository).saveAll(anyList());
  }

  @Test
  void store_nullList_expectFail() {
    assertThrows(NullPointerException.class, () -> service.store(null, "1"));
  }

  @Test
  void store_nullDatasetId_expectFail() {
    assertThrows(NullPointerException.class, () -> service.store(List.of(), null));
  }

  @Test
  void remove_expectSuccess() {
    var thumb1 = new ThumbnailIdEntity("1", "t1");
    var thumb2 = new ThumbnailIdEntity("1", "t2");
    var thumb3 = new ThumbnailIdEntity("1", "t3");
    var thumb4 = new ThumbnailIdEntity("1", "t4");
    var thumb5 = new ThumbnailIdEntity("1", "t5");
    when(thumbnailIdRepository.findByDatasetId("1"))
        .thenReturn(List.of(thumb1, thumb2, thumb3, thumb4, thumb5));

    service.remove("1");

    verify(s3client).deleteObjects(any(DeleteObjectsRequest.class));
    verify(thumbnailIdRepository).deleteByDatasetId("1");
  }

  @Test
  void remove_failToGetThumbnailsByDatasetId_expectFail() {
    when(thumbnailIdRepository.findByDatasetId("1"))
        .thenThrow(new RuntimeException("Failed", new Exception()));
    assertThrows(ServiceException.class, () -> service.remove("1"));
  }

  @Test
  void remove_failToDeleteFromS3_expectFail() {
    var thumbs = getThumbnailList();
    when(thumbnailIdRepository.findByDatasetId("1"))
        .thenReturn(thumbs);
    when(s3client.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenThrow(new MultiObjectDeleteException(List.of(), List.of()));

    assertThrows(ThumbnailRemoveException.class, () -> service.remove("1"));
  }

  @Test
  void remove_s3_SdkClientException_expectFail() {
    var thumbs = getThumbnailList();
    when(thumbnailIdRepository.findByDatasetId("1"))
        .thenReturn(thumbs);
    when(s3client.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenThrow(new SdkClientException("Failed"));

    assertThrows(ThumbnailRemoveException.class, () -> service.remove("1"));
  }

  @Test
  void remove_failToDeleteFromLocalRecord_expectFail() {
    var thumbs = getThumbnailList();
    when(thumbnailIdRepository.findByDatasetId("1"))
        .thenReturn(thumbs);
    doThrow(new RuntimeException("Failed")).when(thumbnailIdRepository).deleteByDatasetId("1");
    assertThrows(ServiceException.class, () -> service.remove("1"));
  }

  @Test
  void remove_nullDatasetId_expectFail() {
    assertThrows(NullPointerException.class, () -> service.remove(null));
  }

  private List<ThumbnailIdEntity> getThumbnailList() {
    var thumb1 = new ThumbnailIdEntity("1", "t1");
    var thumb2 = new ThumbnailIdEntity("1", "t2");
    var thumb3 = new ThumbnailIdEntity("1", "t3");
    var thumb4 = new ThumbnailIdEntity("1", "t4");
    var thumb5 = new ThumbnailIdEntity("1", "t5");
    return List.of(thumb1, thumb2, thumb3, thumb4, thumb5);
  }
}