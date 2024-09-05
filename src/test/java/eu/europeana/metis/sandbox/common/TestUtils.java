package eu.europeana.metis.sandbox.common;

import static org.junit.jupiter.api.Assertions.fail;

import eu.europeana.metis.harvesting.FullRecord;
import eu.europeana.metis.harvesting.FullRecordHarvestingIterator;
import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
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
        final Predicate<OaiRecordHeader> filter) throws HarvesterException {
      for (OaiRecordHeader item : this.source) {
        try {
          action.process(item);
        } catch (IOException e) {
          throw new HarvesterException(e.getMessage(), e);
        }
      }
    }

    @Override
    public void forEachNonDeleted(ReportingIteration<OaiRecordHeader> reportingIteration) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Integer countRecords() {
      return source.size();
    }

    @Override
    public void close() {
    }
  }

  public static class TestHttpRecordIterator implements
      FullRecordHarvestingIterator<FullRecord, Path> {

    private final List<FullRecord> extractedDirectory;

    public TestHttpRecordIterator(List<FullRecord> extractedDirectory) {
      this.extractedDirectory = extractedDirectory;
    }

    @Override
    public void forEachFiltered(ReportingIteration<FullRecord> reportingIteration, Predicate<Path> predicate)
        throws HarvesterException {
      for (FullRecord item : this.extractedDirectory) {
        try {
          reportingIteration.process(item);
        } catch (IOException e) {
          throw new HarvesterException(e.getMessage(), e);
        }
      }
    }

    @Override
    public void forEachNonDeleted(ReportingIteration<FullRecord> reportingIteration) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Integer countRecords() {
      return extractedDirectory.size();
    }

    @Override
    public void close() {
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
