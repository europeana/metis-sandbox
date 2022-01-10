package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
class DatasetGeneratorServiceImpl implements DatasetGeneratorService {

  private final XmlRecordProcessorService xmlRecordProcessorService;

  public DatasetGeneratorServiceImpl(XmlRecordProcessorService xmlRecordProcessorService) {
    this.xmlRecordProcessorService = xmlRecordProcessorService;
  }

  @Override
  public Dataset generate(
      final String id,
      final String name,
      final Country country,
      final Language language,
      final List<ByteArrayInputStream> records) {
    requireNonNull(name, "Dataset name must not be null");
    requireNonNull(country, "Country must not be null");
    requireNonNull(language, "Language must not be null");
    requireNonNull(records, "Records must not be null");
    checkArgument(!records.isEmpty(), "Records must not be empty");

    int totalRecords = records.size();
    Set<Record> recordObjectList =
        records.stream()
            .map(ByteArrayInputStream::readAllBytes)
            .map(
                recordItem ->
                    getOptionalRecordFromProcessorService(id, name, country, language, recordItem))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toSet());

    // recordObjectList will remove duplicate records based on Record equals
    return new Dataset(id, recordObjectList, totalRecords - recordObjectList.size());
  }

  private Optional<Record> getOptionalRecordFromProcessorService(
      final String id,
      final String name,
      final Country country,
      final Language language,
      final byte[] recordData) {
    try {
      return Optional.of(
          Record.builder()
              .recordId(xmlRecordProcessorService.getRecordId(recordData))
              .datasetId(id)
              .datasetName(name)
              .country(country)
              .language(language)
              .content(recordData)
              .build());
    } catch (RecordParsingException recordParsingException) {
      return Optional.empty();
    }
  }
}
