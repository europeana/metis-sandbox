package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpHarvesterImpl;
import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import eu.europeana.metis.utils.ZipFileReader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
class ZipServiceImpl implements ZipService {

  private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("http", "https", "file");
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
