package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.HarvestProtocol;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.FileHarvestingDTO;
import eu.europeana.metis.sandbox.dto.HarvestingParametersDTO;
import eu.europeana.metis.sandbox.dto.HttpHarvestingDTO;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestingDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.HarvestingParameterEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.HarvestingParameterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class HarvestParameterServiceImplTest {

    @Mock
    private HarvestingParameterRepository harvestingParameterRepository;

    @Mock
    private DatasetRepository datasetRepository;

    @InjectMocks
    private HarvestingParameterService harvestingParameterService;

    @Captor
    ArgumentCaptor<HarvestingParameterEntity> entityArgumentCaptor;

    @Test
    void createDatasetHarvestingParameters_fileHarvesting_expectSuccess(){
        HarvestingParameterEntity entity = new HarvestingParameterEntity();
        DatasetEntity datasetEntity = new DatasetEntity();
        datasetEntity.setDatasetId(1);
        when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
        when(harvestingParameterRepository.save(entityArgumentCaptor.capture())).thenReturn(entity);
        HarvestingParametersDTO harvestingParametersDto = new FileHarvestingDTO("fileName", "fileType");

        harvestingParameterService.createDatasetHarvestingParameters("1", harvestingParametersDto);

        HarvestingParameterEntity capturedEntity = entityArgumentCaptor.getValue();

        assertEquals(1, capturedEntity.getDatasetId().getDatasetId());
        assertEquals(HarvestProtocol.FILE, capturedEntity.getProtocol());
        assertEquals("fileName", capturedEntity.getFileName());
        assertEquals("fileType", capturedEntity.getFileType());
        assertNull(capturedEntity.getUrl());
        assertNull(capturedEntity.getSetSpec());
        assertNull(capturedEntity.getMetadataFormat());

    }

    @Test
    void createDatasetHarvestingParameters_httpHarvesting_expectSuccess(){
        HarvestingParameterEntity entity = new HarvestingParameterEntity();
        DatasetEntity datasetEntity = new DatasetEntity();
        datasetEntity.setDatasetId(1);
        when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
        when(harvestingParameterRepository.save(entityArgumentCaptor.capture())).thenReturn(entity);
        HarvestingParametersDTO harvestingParametersDto = new HttpHarvestingDTO("http://url-to-test.com");

        harvestingParameterService.createDatasetHarvestingParameters("1", harvestingParametersDto);

        HarvestingParameterEntity capturedEntity = entityArgumentCaptor.getValue();

        assertEquals(1, capturedEntity.getDatasetId().getDatasetId());
        assertEquals(HarvestProtocol.HTTP, capturedEntity.getProtocol());
        assertEquals("http://url-to-test.com", capturedEntity.getUrl());
        assertNull(capturedEntity.getFileName());
        assertNull(capturedEntity.getFileType());
        assertNull(capturedEntity.getSetSpec());
        assertNull(capturedEntity.getMetadataFormat());

    }

    @Test
    void createDatasetHarvestingParameters_OAIHarvesting_expectSuccess(){
        HarvestingParameterEntity entity = new HarvestingParameterEntity();
        DatasetEntity datasetEntity = new DatasetEntity();
        datasetEntity.setDatasetId(1);
        when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
        when(harvestingParameterRepository.save(entityArgumentCaptor.capture())).thenReturn(entity);
        HarvestingParametersDTO harvestingParametersDto = new OAIPmhHarvestingDTO("http://url-to-test.com", "setSpec", "metadataFormat");

        harvestingParameterService.createDatasetHarvestingParameters("1", harvestingParametersDto);

        HarvestingParameterEntity capturedEntity = entityArgumentCaptor.getValue();

        assertEquals(1, capturedEntity.getDatasetId().getDatasetId());
        assertEquals(HarvestProtocol.OAI_PMH, capturedEntity.getProtocol());
        assertEquals("http://url-to-test.com", capturedEntity.getUrl());
        assertEquals("setSpec", capturedEntity.getSetSpec());
        assertEquals("metadataFormat", capturedEntity.getMetadataFormat());
        assertNull(capturedEntity.getFileName());
        assertNull(capturedEntity.getFileType());


    }

    @Test
    void createDatasetHarvestingParameters_expectFail(){

        when(harvestingParameterRepository.save(any())).thenThrow(new RuntimeException());
        HarvestingParametersDTO harvestingParametersDto = new FileHarvestingDTO("fileName", "fileType");

        assertThrows(ServiceException.class, () ->
                harvestingParameterService.createDatasetHarvestingParameters("1", harvestingParametersDto));

    }

    @Test
    void getDatasetHarvestingParameters_expectSuccess(){
        DatasetEntity datasetEntity = new DatasetEntity();
        datasetEntity.setDatasetId(1);
        HarvestingParameterEntity entity = new HarvestingParameterEntity(datasetEntity, HarvestProtocol.FILE,
                "fileName", "fileType", null, null, null);
        when(harvestingParameterRepository.getHarvestingParametersEntitiesByDatasetId_DatasetId(1)).thenReturn(entity);

        HarvestingParameterEntity resultEntity = harvestingParameterService.getDatasetHarvestingParameters("1");

        assertEquals(entity, resultEntity);
    }

    @Test
    void remove_expectSuccess(){
        harvestingParameterService.remove("1");
        verify(harvestingParameterRepository).deleteByDatasetIdDatasetId(1);
    }

    @Test
    void remove_expectFail(){
        doThrow(new RuntimeException("error")).when(harvestingParameterRepository).deleteByDatasetIdDatasetId(1);
        assertThrows(ServiceException.class, () -> harvestingParameterService.remove("1"));
    }

}
