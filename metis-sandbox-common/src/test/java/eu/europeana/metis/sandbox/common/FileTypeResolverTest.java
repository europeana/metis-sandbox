package eu.europeana.metis.sandbox.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.InvalidCompressedFileException;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.multipart.MultipartFile;

class FileTypeResolverTest {

  @ParameterizedTest
  @MethodSource
  void fromMultipart_withValidContentType_shouldReturnExtension(String contentType, CompressedFileExtension expectedExtension) {
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getContentType()).thenReturn(contentType);
    when(multipartFile.getOriginalFilename()).thenReturn("file.zip");

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromMultipart(multipartFile);
    assertEquals(expectedExtension, compressedFileExtension);
  }

  private static Stream<Arguments> fromMultipart_withValidContentType_shouldReturnExtension() {
    return Stream.of(
        of("application/zip", CompressedFileExtension.ZIP),
        of("application/gzip", CompressedFileExtension.GZIP),
        of("application/x-tar", CompressedFileExtension.TAR),
        of("application/x-zip-compressed", CompressedFileExtension.ZIP),
        of("application/x-gzip", CompressedFileExtension.GZIP)
    );
  }

  @Test
  void fromMultipart_withUnknownContentTypeButValidExtension_shouldFallbackToFilename() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getContentType()).thenReturn("application/octet-stream");
    when(multipartFile.getOriginalFilename()).thenReturn("archive.tar");

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromMultipart(multipartFile);
    assertEquals(CompressedFileExtension.TAR, compressedFileExtension);
  }

  @Test
  void fromMultipart_withEmptyContentTypeButValidExtension_shouldFallbackToFilename() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getOriginalFilename()).thenReturn("archive.tar");

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromMultipart(multipartFile);
    assertEquals(CompressedFileExtension.TAR, compressedFileExtension);
  }

  @Test
  void fromMultipart_withEmptyContentTypeAndEmptyExtension_shouldThrowException() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromMultipart(multipartFile));
  }

  @Test
  void fromMultipart_withInvalidFile_shouldThrowException() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getContentType()).thenReturn("text/plain");
    when(multipartFile.getOriginalFilename()).thenReturn("readme.txt");

    assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromMultipart(multipartFile));
  }

  @ParameterizedTest
  @MethodSource
  void fromUrl_withValidContentType_shouldReturnExtension(String urlString, String contentType,
      CompressedFileExtension expectedExtension)
      throws Exception {
    URI uri = spy(new URI(urlString));
    URL url = spy(uri.toURL());
    when(uri.toURL()).thenReturn(url);
    URLConnection connection = mock(URLConnection.class);
    doReturn(connection).when(url).openConnection();
    when(connection.getContentType()).thenReturn(contentType);

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromUrl(uri);
    assertEquals(expectedExtension, compressedFileExtension);
  }

  private static Stream<Arguments> fromUrl_withValidContentType_shouldReturnExtension() {
    return Stream.of(
        of("http://example.com/readme.zip", "application/zip", CompressedFileExtension.ZIP),
        of("http://example.com/readme.gz", "application/gzip", CompressedFileExtension.GZIP),
        of("http://example.com/readme.tar", "application/x-tar", CompressedFileExtension.TAR),
        of("http://example.com/readme.zip", "application/x-zip-compressed", CompressedFileExtension.ZIP),
        of("http://example.com/readme.gz", "application/x-gzip", CompressedFileExtension.GZIP),
        //Wrong content type, but valid extension
        of("http://example.com/readme.zip", "application/octet-stream", CompressedFileExtension.ZIP),
        of("http://example.com/readme.gz", "application/octet-stream", CompressedFileExtension.GZIP),
        of("http://example.com/readme.tar", "application/octet-stream", CompressedFileExtension.TAR)
    );
  }

  @Test
  void fromUrl_withEmptyContentTypeButValidExtension_shouldFallbackToFilename() throws Exception {
    URI uri = spy(new URI("http://example.com/readme.zip"));
    URL url = spy(uri.toURL());
    when(uri.toURL()).thenReturn(url);
    when(uri.getPath()).thenReturn("file.zip");
    URLConnection connection = mock(URLConnection.class);
    doReturn(connection).when(url).openConnection();

    CompressedFileExtension compressedFileExtension = FileTypeResolver.fromUrl(uri);

    assertEquals(CompressedFileExtension.ZIP, compressedFileExtension);
  }

  @Test
  void fromUrl_withEmptyContentTypeAndEmptyExtension_shouldThrowException() throws Exception {
    URI uri = spy(new URI("http://example.com/"));
    URL url = spy(uri.toURL());
    when(uri.toURL()).thenReturn(url);
    URLConnection connection = mock(URLConnection.class);
    doReturn(connection).when(url).openConnection();

    assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromUrl(uri));
  }

  @Test
  void fromUrl_withInvalidFile_shouldThrowException() {
    URI uri = URI.create("http://example.com/readme.txt");
    assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromUrl(uri));
  }

  @Test
  void fromUrl_withInvalidLink_shouldThrowException() throws Exception {
    URI uri = spy(new URI("http://example.com/readme.txt"));
    when(uri.toURL()).thenThrow(new MalformedURLException("Invalid link"));

    assertThrows(InvalidCompressedFileException.class, () -> FileTypeResolver.fromUrl(uri));
  }
}

