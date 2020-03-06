package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultDatasetServiceTest {

  @Mock
  private DatasetGeneratorService generatorService;

  @Mock
  private AsyncDatasetPublishService publishService;

  @Mock
  private DatasetRepository datasetRepository;

  @InjectMocks
  private DefaultDatasetService service;

  @Test
  void createDataset_expectSuccess() {

    var records = List.of("record1");
    var record = Record.builder().datasetId("").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("").recordId("").build();
    var dataset = new Dataset("1234_name", List.of(record));

    when(generatorService.generate("name", Country.AUSTRIA, Language.BE, records))
        .thenReturn(dataset);
    var datasetId = service.createDataset("name", Country.AUSTRIA, Language.BE, records);

    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
    verify(publishService, times(1)).publish(dataset);

    assertEquals("1234_name", datasetId);
  }

  @Test
  void createDataset_nullValue_expectFail() {
    assertThrows(NullPointerException.class,
        () -> service.createDataset(null, Country.AUSTRIA, Language.BE, List.of()));
  }

  @Test
  void createDataset_emptyRecordList_expectFail() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createDataset("name", Country.AUSTRIA, Language.BE, List.of()));
  }

  @Test
  void createDataset_saveFail_expectSuccess() {

    var records = List.of("record1");
    var record = Record.builder().datasetId("").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("").recordId("").build();
    var dataset = new Dataset("1234_name", List.of(record));

    when(generatorService.generate("name", Country.AUSTRIA, Language.BE, records))
        .thenReturn(dataset);
    when(datasetRepository.save(any(DatasetEntity.class)))
        .thenThrow(new IllegalArgumentException());
    assertThrows(ServiceException.class,
        () -> service.createDataset("name", Country.AUSTRIA, Language.BE, records));

    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
    verify(publishService, never()).publish(dataset);
  }
}