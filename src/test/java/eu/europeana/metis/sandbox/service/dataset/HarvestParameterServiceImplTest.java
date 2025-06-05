//package eu.europeana.metis.sandbox.service.dataset;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import eu.europeana.metis.sandbox.dto.harvest.HarvestProtocol;
//import eu.europeana.metis.sandbox.common.exception.ServiceException;
//import eu.europeana.metis.sandbox.dto.harvest.FileHarvestDTO;
//import eu.europeana.metis.sandbox.dto.harvest.HarvestParametersDTO;
//import eu.europeana.metis.sandbox.dto.harvest.HttpHarvestDTO;
//import eu.europeana.metis.sandbox.dto.OAIPmhHarvestDTO;
//import eu.europeana.metis.sandbox.entity.DatasetEntity;
//import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
//import eu.europeana.metis.sandbox.repository.DatasetRepository;
//import eu.europeana.metis.sandbox.repository.HarvestingParameterRepository;
//import java.util.Optional;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//
//@ExtendWith(SpringExtension.class)
//class HarvestParameterServiceImplTest {
//
//    @Mock
//    private HarvestingParameterRepository harvestingParameterRepository;
//
//    @Mock
//    private DatasetRepository datasetRepository;
//
//    @InjectMocks
//    private HarvestingParameterService harvestingParameterService;
//
//    @Captor
//    ArgumentCaptor<HarvestParametersEntity> entityArgumentCaptor;
//
//    @Test
//    void createDatasetHarvestingParameters_fileHarvesting_expectSuccess(){
//        HarvestParametersEntity entity = new HarvestParametersEntity();
//        DatasetEntity datasetEntity = new DatasetEntity();
//        datasetEntity.setDatasetId(1);
//        when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
//        when(harvestingParameterRepository.save(entityArgumentCaptor.capture())).thenReturn(entity);
//        HarvestParametersDTO harvestParametersDto = new FileHarvestDTO("fileName", "fileType", new byte[0], xsltFile.getBytes());
//
//        harvestingParameterService.createDatasetHarvestParameters("1", harvestParametersDto);
//
//        HarvestParametersEntity capturedEntity = entityArgumentCaptor.getValue();
//
//        assertEquals(1, capturedEntity.getDatasetId().getDatasetId());
//        assertEquals(HarvestProtocol.FILE, capturedEntity.getHarvestProtocol());
//        assertEquals("fileName", capturedEntity.getFileName());
//        assertEquals("fileType", capturedEntity.getFileType());
//        assertNull(capturedEntity.getUrl());
//        assertNull(capturedEntity.getSetSpec());
//        assertNull(capturedEntity.getMetadataFormat());
//
//    }
//
//    @Test
//    void createDatasetHarvestingParameters_httpHarvesting_expectSuccess(){
//        HarvestParametersEntity entity = new HarvestParametersEntity();
//        DatasetEntity datasetEntity = new DatasetEntity();
//        datasetEntity.setDatasetId(1);
//        when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
//        when(harvestingParameterRepository.save(entityArgumentCaptor.capture())).thenReturn(entity);
//        HarvestParametersDTO harvestParametersDto = new HttpHarvestDTO("http://url-to-test.com");
//
//        harvestingParameterService.createDatasetHarvestParameters("1", harvestParametersDto);
//
//        HarvestParametersEntity capturedEntity = entityArgumentCaptor.getValue();
//
//        assertEquals(1, capturedEntity.getDatasetId().getDatasetId());
//        assertEquals(HarvestProtocol.HTTP, capturedEntity.getHarvestProtocol());
//        assertEquals("http://url-to-test.com", capturedEntity.getUrl());
//        assertNull(capturedEntity.getFileName());
//        assertNull(capturedEntity.getFileType());
//        assertNull(capturedEntity.getSetSpec());
//        assertNull(capturedEntity.getMetadataFormat());
//
//    }
//
//    @Test
//    void createDatasetHarvestingParameters_OAIHarvesting_expectSuccess(){
//        HarvestParametersEntity entity = new HarvestParametersEntity();
//        DatasetEntity datasetEntity = new DatasetEntity();
//        datasetEntity.setDatasetId(1);
//        when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
//        when(harvestingParameterRepository.save(entityArgumentCaptor.capture())).thenReturn(entity);
//        HarvestParametersDTO harvestParametersDto = new OAIPmhHarvestDTO("http://url-to-test.com", "setSpec", "metadataFormat");
//
//        harvestingParameterService.createDatasetHarvestParameters("1", harvestParametersDto);
//
//        HarvestParametersEntity capturedEntity = entityArgumentCaptor.getValue();
//
//        assertEquals(1, capturedEntity.getDatasetId().getDatasetId());
//        assertEquals(HarvestProtocol.OAI_PMH, capturedEntity.getHarvestProtocol());
//        assertEquals("http://url-to-test.com", capturedEntity.getUrl());
//        assertEquals("setSpec", capturedEntity.getSetSpec());
//        assertEquals("metadataFormat", capturedEntity.getMetadataFormat());
//        assertNull(capturedEntity.getFileName());
//        assertNull(capturedEntity.getFileType());
//
//
//    }
//
//    @Test
//    void createDatasetHarvestingParameters_expectFail(){
//
//        when(harvestingParameterRepository.save(any())).thenThrow(new RuntimeException());
//        HarvestParametersDTO harvestParametersDto = new FileHarvestDTO("fileName", "fileType", new byte[0], xsltFile.getBytes());
//
//        assertThrows(ServiceException.class, () ->
//                harvestingParameterService.createDatasetHarvestParameters("1", harvestParametersDto));
//
//    }
//
//    @Test
//    void getDatasetHarvestingParameters_expectSuccess(){
//        DatasetEntity datasetEntity = new DatasetEntity();
//        datasetEntity.setDatasetId(1);
//        HarvestParametersEntity entity = new HarvestParametersEntity();
//        entity.setDatasetId(datasetEntity);
//        entity.setHarvestProtocol(HarvestProtocol.HTTP);
//        entity.setFileName("fileName");
//        entity.setFileType("fileType");
//        when(harvestingParameterRepository.getHarvestingParametersEntitiesByDatasetId_DatasetId(1)).thenReturn(entity);
//
//        HarvestParametersEntity resultEntity = harvestingParameterService.getDatasetHarvestingParameters("1");
//
//        assertEquals(entity, resultEntity);
//    }
//
//    @Test
//    void remove_expectSuccess(){
//        harvestingParameterService.remove("1");
//        verify(harvestingParameterRepository).deleteByDatasetIdDatasetId(1);
//    }
//
//    @Test
//    void remove_expectFail(){
//        doThrow(new RuntimeException("error")).when(harvestingParameterRepository).deleteByDatasetIdDatasetId(1);
//        assertThrows(ServiceException.class, () -> harvestingParameterService.remove("1"));
//    }
//
//}
