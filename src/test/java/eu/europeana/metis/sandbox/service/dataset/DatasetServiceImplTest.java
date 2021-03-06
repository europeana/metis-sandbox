package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import eu.europeana.metis.sandbox.entity.projection.DatasetIdView;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetServiceImplTest {

  @Mock
  private DatasetGeneratorService generatorService;

  @Mock
  private AsyncDatasetPublishService publishService;

  @Mock
  private DatasetRepository datasetRepository;

  @InjectMocks
  private DatasetServiceImpl service;

  @Test
  void createDataset_expectSuccess() {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId("").build();
    var dataset = new Dataset("1234", Set.of(record), 0);
    var datasetEntity = new DatasetEntity("name", 5, Language.NL, Country.NETHERLANDS);
    datasetEntity.setDatasetId(1);

    when(datasetRepository.save(any(DatasetEntity.class))).thenReturn(datasetEntity);
    when(generatorService.generate("1", "name", Country.AUSTRIA, Language.BE, records))
        .thenReturn(dataset);
    var result = service.createDataset("name", Country.AUSTRIA, Language.BE, records);

    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
    verify(publishService, times(1)).publish(dataset);

    assertEquals("1234", result.getDatasetId());
  }

  @Test
  void createDataset_withDuplicateRecords_expectSuccess() {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId("").build();
    var dataset = new Dataset("1234", Set.of(record), 0);
    var datasetEntity = new DatasetEntity("name", 1, Language.NL, Country.NETHERLANDS);
    datasetEntity.setDatasetId(1);

    when(datasetRepository.save(any(DatasetEntity.class))).thenReturn(datasetEntity);
    when(generatorService.generate("1", "name", Country.AUSTRIA, Language.BE, records))
        .thenReturn(dataset);
    var result = service.createDataset("name", Country.AUSTRIA, Language.BE, records);

    verify(datasetRepository, times(2)).save(any(DatasetEntity.class));
    verify(publishService, times(1)).publish(dataset);

    assertEquals("1234", result.getDatasetId());
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
  void createDataset_saveFail_expectFail() {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId("").build();
    var dataset = new Dataset("1234", Set.of(record), 0);

    when(datasetRepository.save(any(DatasetEntity.class)))
        .thenThrow(new IllegalArgumentException());
    assertThrows(ServiceException.class,
        () -> service.createDataset("name", Country.AUSTRIA, Language.BE, records));

    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
    verify(publishService, never()).publish(dataset);
  }

  @Test
  void createDataset_withDuplicateRecords_updateFail_expectFail() {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId("").build();
    var dataset = new Dataset("1234", Set.of(record), 0);
    var datasetEntity = new DatasetEntity("name", 1, Language.NL, Country.NETHERLANDS);
    datasetEntity.setDatasetId(1);

    when(datasetRepository.save(any(DatasetEntity.class)))
        .thenReturn(datasetEntity)
        .thenThrow(new RuntimeException("Failed"));
    when(generatorService.generate("1", "name", Country.AUSTRIA, Language.BE, records))
        .thenReturn(dataset);

    assertThrows(ServiceException.class,
        () -> service.createDataset("name", Country.AUSTRIA, Language.BE, records));

    verify(datasetRepository, times(2)).save(any(DatasetEntity.class));
    verify(publishService, never()).publish(dataset);
  }

  @Test
  void remove_expectSuccess() {
    service.remove("1");
    verify(datasetRepository).deleteById(1);
  }

  @Test
  void remove_fail() {
    doThrow(new RuntimeException("", new Exception())).when(datasetRepository).deleteById(1);
    assertThrows(ServiceException.class, () -> service.remove("1"));
  }

  @Test
  void getDatasetIdsBefore_expectSuccess() {
    var id1 = new DatasetIdViewImpl(1);
    var id2 = new DatasetIdViewImpl(2);
    var id3 = new DatasetIdViewImpl(3);
    var id4 = new DatasetIdViewImpl(4);

    when(datasetRepository.getByCreatedDateBefore(any(LocalDateTime.class)))
        .thenReturn(List.of(id1, id2, id3, id4));

    var result = service.getDatasetIdsCreatedBefore(7);

    assertEquals(List.of("1", "2", "3", "4"), result);
  }

  @Test
  void getDatasetIdsBefore_failToGetIds_expectFail() {
    when(datasetRepository.getByCreatedDateBefore(any(LocalDateTime.class)))
        .thenThrow(new RuntimeException("Issue"));

    assertThrows(ServiceException.class, () -> service.getDatasetIdsCreatedBefore(7));
  }

  private static class DatasetIdViewImpl implements DatasetIdView {

    private final Integer datasetId;

    public DatasetIdViewImpl(Integer datasetId) {
      this.datasetId = datasetId;
    }

    @Override
    public Integer getDatasetId() {
      return datasetId;
    }
  }
}