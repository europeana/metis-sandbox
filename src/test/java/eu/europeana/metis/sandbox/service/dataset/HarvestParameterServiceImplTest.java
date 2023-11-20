package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.HarvestProtocol;
import eu.europeana.metis.sandbox.dto.FileHarvestingDto;
import eu.europeana.metis.sandbox.dto.HarvestingParametricDto;
import eu.europeana.metis.sandbox.dto.HttpHarvestingDto;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestingDto;
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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class HarvestParameterServiceImplTest {

    @Mock
    private HarvestingParameterRepository harvestingParameterRepository;

    @Mock
    private DatasetRepository datasetRepository;

    @InjectMocks
    private HarvestingParameterServiceImpl harvestingParameterService;

    @Captor
    ArgumentCaptor<HarvestingParameterEntity> entityArgumentCaptor;

    @Test
    void createDatasetHarvestingParameters_fileHarvesting_expectSuccess(){
        HarvestingParameterEntity entity = new HarvestingParameterEntity();
        DatasetEntity datasetEntity = new DatasetEntity();
        datasetEntity.setDatasetId(1);
        when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
        when(harvestingParameterRepository.save(entityArgumentCaptor.capture())).thenReturn(entity);
        HarvestingParametricDto harvestingParametricDto = new FileHarvestingDto("fileName", "fileType");

        harvestingParameterService.createDatasetHarvestingParameters("1", harvestingParametricDto);

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
        HarvestingParametricDto harvestingParametricDto = new HttpHarvestingDto("http://url-to-test.com");

        harvestingParameterService.createDatasetHarvestingParameters("1", harvestingParametricDto);

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
        HarvestingParametricDto harvestingParametricDto = new OAIPmhHarvestingDto("http://url-to-test.com", "setSpec", "metadataFormat");

        harvestingParameterService.createDatasetHarvestingParameters("1", harvestingParametricDto);

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
        HarvestingParametricDto harvestingParametricDto = new FileHarvestingDto("fileName", "fileType");

        harvestingParameterService.createDatasetHarvestingParameters("1", harvestingParametricDto);

        HarvestingParameterEntity capturedEntity = entityArgumentCaptor.getValue();

        assertEquals(1, capturedEntity.getDatasetId().getDatasetId());
        assertEquals(HarvestProtocol.FILE, capturedEntity.getProtocol());
        assertEquals("fileName", capturedEntity.getFileName());
        assertEquals("fileType", capturedEntity.getFileType());
        assertNull(capturedEntity.getUrl());
        assertNull(capturedEntity.getSetSpec());
        assertNull(capturedEntity.getMetadataFormat());

    }

}
