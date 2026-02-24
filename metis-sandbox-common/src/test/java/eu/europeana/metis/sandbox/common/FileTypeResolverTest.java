package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.common.exception.InvalidCompressedFileException;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

class FileTypeResolverTest {

  @ParameterizedTest
  @MethodSource
  void fromMultipart_withValidContentType_shouldReturnExtension(String contentType, CompressedFileExtension expectedExtension) {
    MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
    Mockito.when(multipartFile.getContentType()).thenReturn(contentType);
    Mockito.when(multipartFile.getOriginalFilename()).thenReturn("file.zip");

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromMultipart(multipartFile);
    Assertions.assertEquals(expectedExtension, compressedFileExtension);
  }

  private static Stream<Arguments> fromMultipart_withValidContentType_shouldReturnExtension() {
    return Stream.of(
        Arguments.of("application/zip", CompressedFileExtension.ZIP),
        Arguments.of("application/gzip", CompressedFileExtension.GZIP),
        Arguments.of("application/x-tar", CompressedFileExtension.TAR),
        Arguments.of("application/x-zip-compressed", CompressedFileExtension.ZIP),
        Arguments.of("application/x-gzip", CompressedFileExtension.GZIP)
    );
  }

  @Test
  void fromMultipart_withUnknownContentTypeButValidExtension_shouldFallbackToFilename() {
    MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
    Mockito.when(multipartFile.getContentType()).thenReturn("application/octet-stream");
    Mockito.when(multipartFile.getOriginalFilename()).thenReturn("archive.tar");

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromMultipart(multipartFile);
    Assertions.assertEquals(CompressedFileExtension.TAR, compressedFileExtension);
  }

  @Test
  void fromMultipart_withEmptyContentTypeButValidExtension_shouldFallbackToFilename() {
    MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
    Mockito.when(multipartFile.getOriginalFilename()).thenReturn("archive.tar");

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromMultipart(multipartFile);
    Assertions.assertEquals(CompressedFileExtension.TAR, compressedFileExtension);
  }

  @Test
  void fromMultipart_withEmptyContentTypeAndEmptyExtension_shouldThrowException() {
    MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
    Assertions.assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromMultipart(multipartFile));
  }

  @Test
  void fromMultipart_withInvalidFile_shouldThrowException() {
    MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
    Mockito.when(multipartFile.getContentType()).thenReturn("text/plain");
    Mockito.when(multipartFile.getOriginalFilename()).thenReturn("readme.txt");

    Assertions.assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromMultipart(multipartFile));
  }

  @ParameterizedTest
  @MethodSource
  void fromUrl_withValidContentType_shouldReturnExtension(String urlString, String contentType, CompressedFileExtension expectedExtension)
      throws Exception {
    URI uri = Mockito.spy(new URI(urlString));
    URL url = Mockito.spy(uri.toURL());
    Mockito.when(uri.toURL()).thenReturn(url);
    URLConnection connection = Mockito.mock(URLConnection.class);
    Mockito.doReturn(connection).when(url).openConnection();
    Mockito.when(connection.getContentType()).thenReturn(contentType);

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromUrl(uri);
    Assertions.assertEquals(expectedExtension, compressedFileExtension);
  }

  private static Stream<Arguments> fromUrl_withValidContentType_shouldReturnExtension() {
    return Stream.of(
        Arguments.of("http://example.com/readme.zip", "application/zip", CompressedFileExtension.ZIP),
        Arguments.of("http://example.com/readme.gz", "application/gzip", CompressedFileExtension.GZIP),
        Arguments.of("http://example.com/readme.tar", "application/x-tar", CompressedFileExtension.TAR),
        Arguments.of("http://example.com/readme.zip", "application/x-zip-compressed", CompressedFileExtension.ZIP),
        Arguments.of("http://example.com/readme.gz", "application/x-gzip", CompressedFileExtension.GZIP),
        //Wrong content type, but valid extension
        Arguments.of("http://example.com/readme.zip", "application/octet-stream", CompressedFileExtension.ZIP),
        Arguments.of("http://example.com/readme.gz", "application/octet-stream", CompressedFileExtension.GZIP),
        Arguments.of("http://example.com/readme.tar", "application/octet-stream", CompressedFileExtension.TAR)
    );
  }

  @Test
  void fromUrl_withEmptyContentTypeButValidExtension_shouldFallbackToFilename() throws Exception {
    URI uri = Mockito.spy(new URI("http://example.com/readme.zip"));
    URL url = Mockito.spy(uri.toURL());
    Mockito.when(uri.toURL()).thenReturn(url);
    Mockito.when(uri.getPath()).thenReturn("file.zip");
    URLConnection connection = Mockito.mock(URLConnection.class);
    Mockito.doReturn(connection).when(url).openConnection();

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromUrl(uri);

    Assertions.assertEquals(CompressedFileExtension.ZIP, compressedFileExtension);
  }

  @Test
  void fromUrl_withEmptyContentTypeAndEmptyExtension_shouldThrowException() throws Exception {
    URI uri = Mockito.spy(new URI("http://example.com/"));
    URL url = Mockito.spy(uri.toURL());
    Mockito.when(uri.toURL()).thenReturn(url);
    URLConnection connection = Mockito.mock(URLConnection.class);
    Mockito.doReturn(connection).when(url).openConnection();

    Assertions.assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromUrl(uri));
  }

  @Test
  void fromUrl_withInvalidFile_shouldThrowException() {
    URI uri = URI.create("http://example.com/readme.txt");
    Assertions.assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromUrl(uri));
  }

  @Test
  void fromUrl_withInvalidLink_shouldThrowException() throws Exception {
    URI uri = Mockito.spy(new URI("http://example.com/readme.txt"));
    Mockito.when(uri.toURL()).thenThrow(new MalformedURLException("Invalid link"));

    Assertions.assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromUrl(uri));
  }
}

