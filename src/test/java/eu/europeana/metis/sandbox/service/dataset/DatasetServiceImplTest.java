package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.projection.DatasetIdView;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
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
  private DatasetRepository datasetRepository;

  @Captor
  private ArgumentCaptor<DatasetEntity> captor;

  @InjectMocks
  private DatasetServiceImpl service;


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
    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
  }

  @Test
  void createEmptyDataset_withXslt_expectSuccess(){
    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(1);

    when(datasetRepository.save(captor.capture())).thenReturn(datasetEntity);

    String result = service.createEmptyDataset("datasetName", Country.NETHERLANDS, Language.NL, new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
    assertEquals("1", result);
    assertEquals( "record", captor.getValue().getXsltEdmExternalContent());
    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
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
    verify(datasetRepository, times(1)).save(any(DatasetEntity.class));
  }

  @Test
  void updateNumberOfTotalRecord_expectSuccess(){
    service.updateNumberOfTotalRecord("1", 10);
    verify(datasetRepository).updateRecordsQuantity(1, 10);
  }

  @Test
  void updateRecordsLimitExceededToTrue_expectSuccess(){
    service.setRecordLimitExceeded("1");
    verify(datasetRepository).setRecordLimitExceeded(1);
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