package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.HarvestParametersRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HarvestParameterServiceTest {

  @Mock
  private HarvestParametersRepository harvestParametersRepository;

  @Mock
  private DatasetRepository datasetRepository;

  @InjectMocks
  private HarvestParameterService harvestParameterService;

  @Test
  void createDatasetHarvestParameters() {
    String datasetId = "1";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);
    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(Integer.valueOf(datasetId));

    when(datasetRepository.findById(Integer.valueOf(datasetId))).thenReturn(Optional.of(datasetEntity));
    ArgumentCaptor<HarvestParametersEntity> captor = ArgumentCaptor.forClass(HarvestParametersEntity.class);
    when(harvestParametersRepository.save(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.createDatasetHarvestParameters(datasetId, oaiHarvestParametersDTO);
    assertNotNull(harvestParametersEntity);
    assertInstanceOf(OaiHarvestParametersEntity.class, harvestParametersEntity);

    OaiHarvestParametersEntity oaiHarvestParametersEntity = (OaiHarvestParametersEntity) harvestParametersEntity;
    assertEquals(oaiHarvestParametersDTO.getUrl(), oaiHarvestParametersEntity.getUrl());
    assertEquals(oaiHarvestParametersDTO.getSetSpec(), oaiHarvestParametersEntity.getSetSpec());
    assertEquals(oaiHarvestParametersDTO.getMetadataFormat(), oaiHarvestParametersEntity.getMetadataFormat());
    assertEquals(oaiHarvestParametersDTO.getStepSize(), oaiHarvestParametersEntity.getStepSize());
    assertEquals(datasetEntity, oaiHarvestParametersEntity.getDatasetEntity());
  }

  @Test
  void createDatasetHarvestParameters_shouldThrowServiceException_onError() {
    String datasetId = "999";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);
    when(datasetRepository.findById(Integer.valueOf(datasetId))).thenThrow(new RuntimeException());
    assertThrows(ServiceException.class, () -> harvestParameterService.createDatasetHarvestParameters(datasetId,
        oaiHarvestParametersDTO));
  }

  @Test
  void createDatasetHarvestParameters_NullValues_Exceptions() {
    String datasetId = "1";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);
    assertThrows(NullPointerException.class, () -> harvestParameterService.createDatasetHarvestParameters(null,
        oaiHarvestParametersDTO));
    assertThrows(IllegalArgumentException.class, () -> harvestParameterService.createDatasetHarvestParameters("",
        oaiHarvestParametersDTO));
    assertThrows(NullPointerException.class, () -> harvestParameterService.createDatasetHarvestParameters(datasetId, null));
  }

  @Test
  void getDatasetHarvestingParameters() {
    String datasetId = "1";
    HarvestParametersEntity harvestParametersEntity = new HarvestParametersEntity();
    when(harvestParametersRepository.findByDatasetEntity_DatasetId(Integer.valueOf(datasetId))).thenReturn(
        Optional.of(harvestParametersEntity));

    Optional<HarvestParametersEntity> harvestParametersEntityOptional = harvestParameterService.getDatasetHarvestingParameters(
        datasetId);
    assertTrue(harvestParametersEntityOptional.isPresent());
    assertEquals(harvestParametersEntity, harvestParametersEntityOptional.get());
  }

  @Test
  void getDatasetHarvestingParameters_shouldReturnEmptyIfNotFound() {
    String datasetId = "1";
    when(harvestParametersRepository.findByDatasetEntity_DatasetId(Integer.valueOf(datasetId))).thenReturn(Optional.empty());

    Optional<HarvestParametersEntity> harvestParametersEntityOptional = harvestParameterService.getDatasetHarvestingParameters(
        datasetId);
    assertFalse(harvestParametersEntityOptional.isPresent());
  }

  @Test
  void getHarvestingParametersById() {
    UUID id = UUID.randomUUID();
    HarvestParametersEntity harvestParametersEntity = new HarvestParametersEntity();
    when(harvestParametersRepository.findById(id)).thenReturn(Optional.of(harvestParametersEntity));

    Optional<HarvestParametersEntity> harvestParametersOptional = harvestParameterService.getHarvestingParametersById(id);
    assertTrue(harvestParametersOptional.isPresent());
    assertEquals(harvestParametersEntity, harvestParametersOptional.get());
  }

  @Test
  void getHarvestingParametersById_shouldReturnEmptyIfNotFound() {
    UUID id = UUID.randomUUID();
    when(harvestParametersRepository.findById(id)).thenReturn(Optional.empty());

    Optional<HarvestParametersEntity> harvestParametersOptional = harvestParameterService.getHarvestingParametersById(id);
    assertFalse(harvestParametersOptional.isPresent());
  }

  @Test
  void remove() {
    String datasetId = "1";
    assertDoesNotThrow(() -> harvestParameterService.remove(datasetId));
    verify(harvestParametersRepository).deleteByDatasetIdDatasetId(Integer.valueOf(datasetId));
  }

  @Test
  void remove_shouldThrowServiceException_onError() {
    String datasetId = "1";
    doThrow(new RuntimeException()).when(harvestParametersRepository).deleteByDatasetIdDatasetId(Integer.valueOf(datasetId));

    assertThrows(ServiceException.class, () -> harvestParameterService.remove(datasetId));
  }

  @Test
  void remove_NullValues_Exceptions() {
    assertThrows(NullPointerException.class, () -> harvestParameterService.remove(null));
    assertThrows(IllegalArgumentException.class, () -> harvestParameterService.remove(""));
  }
}
