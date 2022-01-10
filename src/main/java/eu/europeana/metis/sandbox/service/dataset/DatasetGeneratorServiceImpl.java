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
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
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
      final String datasetId,
      final String name,
      final Country country,
      final Language language,
      final List<ByteArrayInputStream> records) {
    requireNonNull(name, "Dataset name must not be null");
    requireNonNull(country, "Country must not be null");
    requireNonNull(language, "Language must not be null");
    requireNonNull(records, "Records must not be null");
    checkArgument(!records.isEmpty(), "Records must not be empty");

    Set<Record> recordObjectList =
        records.stream()
            .map(ByteArrayInputStream::readAllBytes)
            .map(
                recordItem ->
                    getOptionalRecordFromProcessorService(
                        datasetId, name, country, language, recordItem))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toSet());

    // recordObjectList will remove duplicate records based on Record equals
    return new Dataset(datasetId, recordObjectList, records.size() - recordObjectList.size());
  }

  private Optional<Record> getOptionalRecordFromProcessorService(
      final String datasetId,
      final String name,
      final Country country,
      final Language language,
      final byte[] recordData) {
    try {
      var recordId = xmlRecordProcessorService.getRecordId(recordData);
      return Optional.of(
          Record.builder()
              .recordId(recordId)
              .europeanaId(EuropeanaIdCreator.constructEuropeanaIdString(recordId, datasetId))
              .datasetId(datasetId)
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
