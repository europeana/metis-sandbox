package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class DatasetGeneratorServiceImpl implements DatasetGeneratorService {

  private final RecordRepository recordRepository;

  public DatasetGeneratorServiceImpl(
      RecordRepository recordRepository) {
    this.recordRepository = recordRepository;
  }

  @Override
  public Dataset generate(String datasetId, String name, Country country, Language language,
      List<ByteArrayInputStream> records) {
    requireNonNull(name, "Dataset name must not be null");
    requireNonNull(country, "Country must not be null");
    requireNonNull(language, "Language must not be null");
    requireNonNull(records, "Records must not be null");
    checkArgument(!records.isEmpty(), "Records must not be empty");

    var recordObjectList = records.stream()
        .map(ByteArrayInputStream::readAllBytes)
        .map(record -> {
          Long recordId = recordRepository.save(new RecordEntity(null, null, datasetId, new String(record))).getId();
          return Record.builder()
              .recordId(recordId)
              .datasetId(datasetId)
              .datasetName(name)
              .country(country)
              .language(language)
              .content(record)
              .build();
        })
        .collect(toSet());

    // recordObjectList will remove duplicate records based on Record equals
    return new Dataset(datasetId, recordObjectList, records.size() - recordObjectList.size());
  }
}
