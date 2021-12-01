package eu.europeana.metis.sandbox.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;

public class TestUtils {

  public String readFileToString(String file) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(file);
    if (inputStream == null) {
      throw new IOException("Failed reading file " + file);
    }
    return new BufferedReader(new InputStreamReader(inputStream)).lines()
        .collect(Collectors.joining("\n"));
  }

  public byte[] readFileToBytes(String file) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(file);
    if (inputStream == null) {
      throw new IOException("Failed reading file " + file);
    }
    return inputStream.readAllBytes();
  }

  public List<ByteArrayInputStream> getContentFromZipFile(InputStream providedZipFile)
      throws IOException {
    String zipFolder = UUID.randomUUID().toString();
    ZipFile zipFile = this.createTempZipFile(providedZipFile, zipFolder);
    List<ByteArrayInputStream> result = null;
    List<InputStream> streams = this.getContentFromZipFile(zipFile);
    List<ByteArrayInputStream> tmpResult = new ArrayList<>(streams.size());

    for (InputStream inputStream : streams) {
      result = tmpResult;
      tmpResult.add(new ByteArrayInputStream(inputStream.readAllBytes()));
    }
    zipFile.close();
    FileUtils.deleteDirectory(new File(zipFolder));

    return result;
  }

  private ZipFile createTempZipFile(InputStream content, String folder) throws IOException {
    File tempFile = File.createTempFile(folder, ".zip");
    FileUtils.copyInputStreamToFile(content, tempFile);
    return new ZipFile(tempFile, 5);
  }

  private List<InputStream> getContentFromZipFile(ZipFile zipFile) throws IOException {
    List<InputStream> result = new ArrayList<>();
    Iterator<? extends ZipEntry> entries = zipFile.stream().iterator();

    while (entries.hasNext()) {
      ZipEntry zipEntry = entries.next();
      if (safeCheck(zipEntry)) {
        result.add(zipFile.getInputStream(zipEntry));
      }
    }
    return result;
  }

  private boolean safeCheck(ZipEntry zipEntry) {
    return !zipEntry.isDirectory() && !zipEntry.getName().startsWith("__MACOSX")
        && !zipEntry.getName().endsWith(".DS_Store");
  }
}
