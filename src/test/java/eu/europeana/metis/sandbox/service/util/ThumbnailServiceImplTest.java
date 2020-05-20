package eu.europeana.metis.sandbox.service.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import eu.europeana.metis.sandbox.domain.Bucket;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThumbnailServiceImplTest {

  @Mock
  private AmazonS3 s3client;

  private ThumbnailServiceImpl service;

  @BeforeEach
  public void setup() {
    service = new ThumbnailServiceImpl(s3client, new Bucket("bucket"), thumbnailRepository);
  }

  @Test
  void store_expectSuccess() {
    var thumbnail1 = mock(Thumbnail.class);
    var thumbnail2 = mock(Thumbnail.class);

    when(thumbnail1.getMimeType()).thenReturn("image/jpg");
    when(thumbnail1.getTargetName()).thenReturn("image1");
    when(thumbnail2.getMimeType()).thenReturn("image/jpg");
    when(thumbnail2.getTargetName()).thenReturn("image2");

    service.store(List.of(thumbnail1, thumbnail2));

    verify(s3client, times(2)).putObject(any(PutObjectRequest.class));
  }

  @Test
  void store_storingThumbnail_expectFail() {
    var thumbnail1 = mock(Thumbnail.class);
    var thumbnail2 = mock(Thumbnail.class);

    when(thumbnail1.getMimeType()).thenReturn("image/jpg");
    when(thumbnail1.getTargetName()).thenReturn("image1");

    when(s3client.putObject(any(PutObjectRequest.class))).thenThrow(new SdkClientException(""));

    assertThrows(ThumbnailStoringException.class,
        () -> service.store(List.of(thumbnail1, thumbnail2)));

    verify(s3client).putObject(any(PutObjectRequest.class));
    verifyNoMoreInteractions(s3client);
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
        () -> service.store(List.of(thumbnail1, thumbnail2)));

    verify(s3client).putObject(any(PutObjectRequest.class));
    verifyNoMoreInteractions(s3client);
  }

  @Test
  void store_nullList_expectFail() {
    assertThrows(NullPointerException.class, () -> service.store(null));
  }
}