package eu.europeana.metis.sandbox.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the functionality of reading zip files.
 * <br /><br />
 * TODO search ZipFileReader class in project metis-common and add a method that also returns
 *   records as List<byte[]> then replace this class with that one
 */
public class ZipFileReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipFileReader.class);

  private static final String MAC_TEMP_FOLDER = "__MACOSX";
  private static final String MAC_TEMP_FILE = ".DS_Store";

  /**
   * This method extracts all files from a ZIP file and returns them as byte arrays. This method
   * only considers files in the main directory. This method creates (and then removes) a temporary
   * file.
   *
   * @param providedZipFile Input stream containing the zip file. This method is not responsible for
   *                        closing the stream.
   * @return A list of records.
   * @throws IOException In case of problems with the temporary file or with reading the zip file.
   */
  public List<byte[]> getRecordsFromZipFile(InputStream providedZipFile)
      throws IOException {

    // Create temporary file.
    var tempFile = getFile(providedZipFile);
    // Get entries as a byte array list.
    return processFile(tempFile);

  }

  private File getFile(InputStream providedZipFile) throws IOException {
    // Create temporary file.
    final String prefix = UUID.randomUUID().toString();
    final File tempFile = File.createTempFile(prefix, ".zip");
    FileUtils.copyInputStreamToFile(providedZipFile, tempFile);
    LOGGER.info("Temp file: {} created.", tempFile);
    return tempFile;
  }

  private List<byte[]> processFile(File tempFile) throws IOException {
    // Open temporary zip file, read it and delete it.
    try (final ZipFile zipFile = new ZipFile(tempFile, ZipFile.OPEN_READ | ZipFile.OPEN_DELETE)) {
      List<byte[]> result = new ArrayList<>();
      var entries = zipFile.stream().iterator();
      while (entries.hasNext()) {
        var zipEntry = entries.next();
        if (reject(zipEntry)) {
          continue;
        }
        result.add(IOUtils.toByteArray(zipFile.getInputStream(zipEntry)));
      }
      return result;
    }
  }

  boolean reject(ZipEntry zipEntry) {
    return zipEntry.isDirectory() || zipEntry.getName().startsWith(MAC_TEMP_FOLDER)
        || zipEntry.getName().endsWith(MAC_TEMP_FILE);
  }
}
