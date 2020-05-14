package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import eu.europeana.metis.utils.ZipFileReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
class ZipServiceImpl implements ZipService {

  private final ZipFileReader zipFileReader;

  public ZipServiceImpl(ZipFileReader zipFileReader) {
    this.zipFileReader = zipFileReader;
  }

  @Override
  public List<ByteArrayInputStream> parse(MultipartFile file) {
    List<ByteArrayInputStream> records;
    try {
      records = zipFileReader.getContentFromZipFile(file.getInputStream());
    } catch (IOException ex) {
      throw new InvalidZipFileException(ex);
    }

    if (records.isEmpty()) {
      throw new IllegalArgumentException("Provided file does not contain any records");
    }

    return records;
  }
}
