package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class DatasetGeneratorServiceImpl implements DatasetGeneratorService {

  private final XmlRecordProcessorService xmlRecordProcessorService;

  public DatasetGeneratorServiceImpl(
      XmlRecordProcessorService xmlRecordProcessorService) {
    this.xmlRecordProcessorService = xmlRecordProcessorService;
  }

  @Override
  public Dataset generate(String id, String name, Country country, Language language,
      List<String> records) {
    requireNonNull(name, "Dataset name must not be null");
    requireNonNull(country, "Country must not be null");
    requireNonNull(language, "Language must not be null");
    requireNonNull(records, "Records must not be null");
    checkArgument(!records.isEmpty(), "Records must not be empty");

    var recordObjectList = records.stream()
        .map(record -> Record.builder()
            .recordId(xmlRecordProcessorService.getRecordId(record))
            .datasetId(id)
            .datasetName(name)
            .country(country)
            .language(language)
            .content(record)
            .build())
        .collect(Collectors.toList());

    return new Dataset(id, recordObjectList);
  }
}
