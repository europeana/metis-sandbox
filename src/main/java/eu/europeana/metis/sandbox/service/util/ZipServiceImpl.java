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

  @Override
  public List<ByteArrayInputStream> parse(String URL) {

    String tmpFolder = System.getProperty("java.io.tmpdir");
    HttpHarvester harvester = new HttpHarvesterImpl();
    List<ByteArrayInputStream> records = new ArrayList<>();

    try {
      Path filePath = downloadFile(URL, Path.of(tmpFolder));

      harvester.harvestRecords(new FileInputStream(filePath.toFile()), CompressedFileExtension.ZIP, entry -> {
        final byte[] content = entry.getEntryContent().readAllBytes();
        records.add(new ByteArrayInputStream(content));
      });

    } catch (IOException | HarvesterException e) {
      throw new IllegalArgumentException(e);
    }
    return records;
  }

  private Path downloadFile(String archiveUrlString, Path downloadDirectory) throws IOException {
    final Path directory = Files.createDirectories(downloadDirectory);
    final Path file = directory.resolve(FilenameUtils.getName(archiveUrlString));
    final URL archiveUrl = new URL(archiveUrlString);
    if (!SUPPORTED_PROTOCOLS.contains(archiveUrl.getProtocol())) {
      throw new IOException("This functionality does not support this protocol ("
              + archiveUrl.getProtocol() + ").");
    }
    // Note: we allow any download URL for http harvesting. This is the functionality we support.
    @SuppressWarnings("findsecbugs:URLCONNECTION_SSRF_FD") final URLConnection conn = archiveUrl.openConnection();
    try (final InputStream inputStream = conn.getInputStream();
         final OutputStream outputStream = Files.newOutputStream(file)) {
      IOUtils.copyLarge(inputStream, outputStream);
    }
    return file;
  }
}
