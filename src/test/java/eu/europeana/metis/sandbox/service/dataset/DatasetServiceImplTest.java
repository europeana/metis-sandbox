package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.projection.DatasetIdView;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

  @Captor
  private ArgumentCaptor<DatasetEntity> captor;

  @InjectMocks
  private DatasetServiceImpl service;

  @Test
  void createDataset_expectSuccess() {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId(1L).build();
    var dataset = new Dataset("1234", Set.of(record), 0);
    var datasetEntity = new DatasetEntity("name", 5, Language.NL, Country.NETHERLANDS, false);
    datasetEntity.setDatasetId(1);

    when(datasetRepository.save(any(DatasetEntity.class))).thenReturn(datasetEntity);
    when(generatorService.generate(any(DatasetMetadata.class), eq(records)))
        .thenReturn(dataset);
    var result = service.createDataset("name", Country.AUSTRIA, Language.BE, records, false);

    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
    verify(publishService, times(1)).publishWithoutXslt(dataset);

    assertEquals("1234", result.getDatasetId());
  }

  @Test
  void createDataset_withXsltContent_expectSuccess() {

    var records = new ArrayList<ByteArrayInputStream>();
    records.add(new ByteArrayInputStream("record1".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId(1L).build();
    var dataset = new Dataset("1234", Set.of(record), 0);
    var datasetEntity = new DatasetEntity("name", 5, Language.NL, Country.NETHERLANDS, false);
    datasetEntity.setDatasetId(1);
    ByteArrayInputStream xsltContent = new ByteArrayInputStream(
        "xsltContent".getBytes(StandardCharsets.UTF_8));

    when(datasetRepository.save(any(DatasetEntity.class))).thenReturn(datasetEntity);
    when(generatorService.generate(any(DatasetMetadata.class), eq(records)))
        .thenReturn(dataset);
    var result = service.createDataset("name", Country.AUSTRIA, Language.BE, records, false,
        xsltContent);

    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
    verify(publishService, times(1)).publishWithXslt(dataset);
    assertEquals("1234", result.getDatasetId());
  }

  @Test
  void createDataset_withDuplicateRecords_expectSuccess() {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId(1L).build();
    var dataset = new Dataset("1234", Set.of(record), 0);
    var datasetEntity = new DatasetEntity("name", 1, Language.NL, Country.NETHERLANDS, false);
    datasetEntity.setDatasetId(1);

    when(datasetRepository.save(any(DatasetEntity.class))).thenReturn(datasetEntity);
    when(generatorService.generate(any(DatasetMetadata.class), eq(records)))
        .thenReturn(dataset);
    var result = service.createDataset("name", Country.AUSTRIA, Language.BE, records, false);

    verify(datasetRepository, times(2)).save(any(DatasetEntity.class));
    verify(publishService, times(1)).publishWithoutXslt(dataset);
    assertEquals("1234", result.getDatasetId());
  }

  @Test
  void createDataset_withDuplicateRecordsAndXsltContent_expectSuccess() {

    var records = new ArrayList<ByteArrayInputStream>();
    records.add(new ByteArrayInputStream("record1".getBytes()));
    records.add(new ByteArrayInputStream("record2".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId(1L).build();
    var dataset = new Dataset("1234", Set.of(record), 0);
    var datasetEntity = new DatasetEntity("name", 1, Language.NL, Country.NETHERLANDS, false);
    datasetEntity.setDatasetId(1);
    ByteArrayInputStream xsltContent = new ByteArrayInputStream(
        "xsltContent".getBytes(StandardCharsets.UTF_8));

    when(datasetRepository.save(any(DatasetEntity.class))).thenReturn(datasetEntity);
    when(generatorService.generate(any(DatasetMetadata.class), eq(records)))
        .thenReturn(dataset);
    var result = service.createDataset("name", Country.AUSTRIA, Language.BE, records, false,
        xsltContent);

    verify(datasetRepository, times(2)).save(any(DatasetEntity.class));
    verify(publishService, times(1)).publishWithXslt(dataset);
    assertEquals("1234", result.getDatasetId());
  }

  @Test
  void createDataset_nullValue_expectFail() {
    assertThrows(NullPointerException.class,
        () -> service.createDataset(null, Country.AUSTRIA, Language.BE, List.of(), false));
  }

  @Test
  void createDataset_emptyRecordList_expectFail() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createDataset("name", Country.AUSTRIA, Language.BE, List.of(), false));
  }

  @Test
  void createDataset_saveFail_expectFail() {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId(1L).build();
    var dataset = new Dataset("1234", Set.of(record), 0);

    when(datasetRepository.save(any(DatasetEntity.class)))
        .thenThrow(new IllegalArgumentException());
    assertThrows(ServiceException.class,
        () -> service.createDataset("name", Country.AUSTRIA, Language.BE, records, false));

    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
    verify(publishService, never()).publishWithoutXslt(dataset);
  }

  @Test
  void createDataset_withDuplicateRecords_updateFail_expectFail() {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));
    var record = Record.builder().datasetId("1").datasetName("").country(Country.AUSTRIA)
        .language(Language.BE).content("".getBytes()).recordId(1L).build();
    var dataset = new Dataset("1234", Set.of(record), 0);
    var datasetEntity = new DatasetEntity("name", 1, Language.NL, Country.NETHERLANDS, false);
    datasetEntity.setDatasetId(1);

    when(datasetRepository.save(any(DatasetEntity.class)))
        .thenReturn(datasetEntity)
        .thenThrow(new RuntimeException("Failed"));
    when(generatorService.generate(any(DatasetMetadata.class), eq(records)))
        .thenReturn(dataset);

    assertThrows(ServiceException.class,
        () -> service.createDataset("name", Country.AUSTRIA, Language.BE, records, false));

    verify(datasetRepository, times(2)).save(any(DatasetEntity.class));
    verify(publishService, never()).publishWithoutXslt(dataset);
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

  @Test
  void createEmptyDataset_withoutXslt_expectSuccess(){
    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(1);
    when(datasetRepository.save(any(DatasetEntity.class))).thenReturn(datasetEntity);

    String result = service.createEmptyDataset("datasetName", Country.NETHERLANDS, Language.NL, new ByteArrayInputStream(new byte[0]));
    assertEquals("1", result);
  }

  @Test
  void createEmptyDataset_withXslt_expectSuccess(){
    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(1);

    when(datasetRepository.save(captor.capture())).thenReturn(datasetEntity);

    String result = service.createEmptyDataset("datasetName", Country.NETHERLANDS, Language.NL, new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
    assertEquals("1", result);
    assertEquals(captor.getValue().getXsltEdmExternalContent(), "record");
  }

  @Test
  void createEmptyDataset_nullDatasetName_expectFail(){
    assertThrows(NullPointerException.class,
            () -> service.createEmptyDataset(null, Country.AUSTRIA, Language.BE, null));
  }

  @Test
  void createEmptyDataset_nullCountry_expectFail(){
    assertThrows(NullPointerException.class,
            () -> service.createEmptyDataset("datasetName", null, Language.BE, null));
  }

  @Test
  void createEmptyDataset_nullLanguage_expectFail(){
    assertThrows(NullPointerException.class,
            () -> service.createEmptyDataset("datasetName", Country.AUSTRIA, null, null));
  }

  @Test
  void createEmptyDataset_exceptionWhileSavingEntity_expectSuccess(){
    when(datasetRepository.save(any(DatasetEntity.class))).thenThrow(new RuntimeException("error test"));

    assertThrows(ServiceException.class,
            () -> service.createEmptyDataset("datasetName", Country.NETHERLANDS, Language.NL, new ByteArrayInputStream(new byte[0])));
  }

  @Test
  void updateNumberOfTotalRecord_expectSuccess(){
    service.updateNumberOfTotalRecord("1", 10);
    verify(datasetRepository).updateRecordsQuantity(1, 10);
  }

  @Test
  void updateRecordsLimitExceededToTrue_expectSuccess(){
    service.updateRecordsLimitExceededToTrue("1");
    verify(datasetRepository).updateRecordLimitExceededToTrue(1);
  }

  @Test
  void updateRecordsLimitExceededToTrue_expectTrue(){
    when(datasetRepository.isXsltPresent(1)).thenReturn(1);
    assertTrue(service.isXsltPresent("1"));

  }

  @Test
  void updateRecordsLimitExceededToTrue_expectFalse(){
    when(datasetRepository.isXsltPresent(1)).thenReturn(0);
    assertFalse(service.isXsltPresent("1"));

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