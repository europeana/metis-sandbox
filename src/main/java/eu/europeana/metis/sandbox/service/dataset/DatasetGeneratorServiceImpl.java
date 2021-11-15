package eu.europeana.metis.sandbox.service.dataset;

import static java.util.stream.Collectors.toSet;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import java.io.ByteArrayInputStream;
import java.util.List;
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
      List<ByteArrayInputStream> records) {

    var totalRecords = records.size();
    var recordObjectList = records.stream()
        .map(ByteArrayInputStream::readAllBytes)
        .map(record -> Record.builder()
            .recordId(xmlRecordProcessorService.getRecordId(record))
            .datasetId(id)
            .datasetName(name)
            .country(country)
            .language(language)
            .content(record)
            .build())
        .collect(toSet());

    // recordObjectList will remove duplicate records based on Record equals
    return new Dataset(id, recordObjectList, totalRecords - recordObjectList.size());
  }
}
