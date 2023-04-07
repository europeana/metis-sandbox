package eu.europeana.metis.sandbox.common;

import static org.junit.jupiter.api.Assertions.fail;

import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TestUtils {

  public String readFileToString(String file) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(file);
    if (inputStream == null) {
      throw new IOException("Failed reading file " + file);
    }
    return new BufferedReader(new InputStreamReader(inputStream))
        .lines()
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

  public static class TestHeaderIterator implements OaiRecordHeaderIterator {

    private final List<OaiRecordHeader> source;

    public TestHeaderIterator(List<OaiRecordHeader> source) {
      this.source = source;
    }

    @Override
    public void forEachFiltered(final ReportingIteration<OaiRecordHeader> action,
        final Predicate<OaiRecordHeader> filter) {
      this.source.forEach(action::process);
    }

    @Override
    public void close() {
    }
  }

  public static class TestHttpRecordIterator implements HttpRecordIterator {

    private final List<Path> extractedDirectory;

    public TestHttpRecordIterator(List<Path> extractedDirectory) {
      this.extractedDirectory = extractedDirectory;
    }

    @Override
    public String getExtractedDirectory() {
      return extractedDirectory.get(0).subpath(0,3).toString();
    }

    @Override
    public void deleteIteratorContent() {
    }

    @Override
    public void forEach(ReportingIteration<Path> action) {
      this.extractedDirectory.forEach(action::process);
    }
  }

  public static void assertContainsOnlyOnce(String expected, String actual) {
    int count = countOccurrences(expected, actual);
    if (count > 1) {
      fail(String.format("There are more than one occurrences of %s in %s", expected, actual));
    }
  }

  private static int countOccurrences(CharSequence sequenceToSearch, CharSequence actual) {
    String strToSearch = sequenceToSearch.toString();
    String strActual = actual.toString();
    int occurrences = 0;

    for (int i = 0; i <= strActual.length() - strToSearch.length(); ++i) {
      if (strActual.substring(i, i + sequenceToSearch.length()).equals(strToSearch)) {
        ++occurrences;
      }
    }

    return occurrences;
  }
}
