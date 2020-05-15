package eu.europeana.metis.sandbox.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import eu.europeana.metis.utils.ZipFileReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ZipServiceImplTest {

  @Mock
  private ZipFileReader reader;

  @InjectMocks
  private ZipServiceImpl service;

  @Test
  void parse_expectSuccess() throws IOException {
    var file = new MockMultipartFile("file", new byte[]{});
    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));

    when(reader.getContentFromZipFile(any(InputStream.class))).thenReturn(records);

    var result = service.parse(file);

    assertEquals(records, result);
  }

  @Test
  void parse_invalidFile_expectFail() throws IOException {
    var file = new MockMultipartFile("file", new byte[]{});

    when(reader.getContentFromZipFile(any(InputStream.class)))
        .thenThrow(new IOException());

    assertThrows(InvalidZipFileException.class, () -> service.parse(file));
  }

  @Test
  void parse_emptyFile_expectFail() throws IOException {
    var file = new MockMultipartFile("file", new byte[]{});

    when(reader.getContentFromZipFile(any(InputStream.class)))
        .thenReturn(List.of());

    assertThrows(IllegalArgumentException.class, () -> service.parse(file));
  }
}