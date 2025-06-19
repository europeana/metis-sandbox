package eu.europeana.metis.sandbox.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.europeana.metis.sandbox.dto.harvest.FileHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.HarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.HttpHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.harvest.FileHarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HttpHarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParametersEntity;
import org.junit.jupiter.api.Test;

class HarvestParametersConverterTest {

  private final DatasetEntity dataset = new DatasetEntity();

  @Test
  void convertToHarvestParametersEntity_OaiHarvestDTO() {
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("https://example.org", "setSpec", "metadataFormat", 1);
    HarvestParametersEntity harvestParametersEntity = HarvestParametersConverter.convertToHarvestParametersEntity(dataset,
        oaiHarvestParametersDTO);

    assertInstanceOf(OaiHarvestParametersEntity.class, harvestParametersEntity);
    OaiHarvestParametersEntity oaiHarvestParametersEntity = (OaiHarvestParametersEntity) harvestParametersEntity;
    assertEquals(oaiHarvestParametersDTO.getUrl(), oaiHarvestParametersEntity.getUrl());
    assertEquals(oaiHarvestParametersDTO.getSetSpec(), oaiHarvestParametersEntity.getSetSpec());
    assertEquals(oaiHarvestParametersDTO.getMetadataFormat(), oaiHarvestParametersEntity.getMetadataFormat());
    assertEquals(oaiHarvestParametersDTO.getStepSize(), oaiHarvestParametersEntity.getStepSize());
    assertEquals(dataset, oaiHarvestParametersEntity.getDatasetEntity());
  }

  @Test
  void convertToHarvestParametersEntity_HttpHarvestDTO() {
    HttpHarvestParametersDTO httpHarvestDTO = new HttpHarvestParametersDTO("https://example.org/file.zip", "file.zip", FileType.ZIP, "data".getBytes(),
        1);
    HarvestParametersEntity harvestParametersEntity = HarvestParametersConverter.convertToHarvestParametersEntity(dataset,
        httpHarvestDTO);

    assertInstanceOf(HttpHarvestParametersEntity.class, harvestParametersEntity);
    HttpHarvestParametersEntity httpHarvestParameters = (HttpHarvestParametersEntity) harvestParametersEntity;
    assertEquals(httpHarvestDTO.getUrl(), httpHarvestParameters.getUrl());
    assertEquals(httpHarvestDTO.getFileName(), httpHarvestParameters.getFileName());
    assertEquals(httpHarvestDTO.getFileType(), httpHarvestParameters.getFileType());
    assertEquals(httpHarvestDTO.getFileContent(), httpHarvestParameters.getFileContent());
    assertEquals(httpHarvestDTO.getStepSize(), httpHarvestParameters.getStepSize());
    assertEquals(dataset, httpHarvestParameters.getDatasetEntity());
  }

  @Test
  void convertToHarvestParametersEntity_FileHarvestDTO() {
    FileHarvestParametersDTO fileHarvestDTO = new FileHarvestParametersDTO("file.zip", FileType.ZIP, "data".getBytes(), 10);
    HarvestParametersEntity harvestParametersEntity = HarvestParametersConverter.convertToHarvestParametersEntity(dataset,
        fileHarvestDTO);

    assertInstanceOf(FileHarvestParametersEntity.class, harvestParametersEntity);
    FileHarvestParametersEntity fileHarvestParameters = (FileHarvestParametersEntity) harvestParametersEntity;
    assertEquals(fileHarvestDTO.getFileName(), fileHarvestParameters.getFileName());
    assertEquals(fileHarvestDTO.getFileType(), fileHarvestParameters.getFileType());
    assertEquals(fileHarvestDTO.getFileContent(), fileHarvestParameters.getFileContent());
    assertEquals(fileHarvestDTO.getStepSize(), fileHarvestParameters.getStepSize());
    assertEquals(dataset, fileHarvestParameters.getDatasetEntity());
  }

  @Test
  void convertToHarvestParametersEntity_UnknownDTO() {
    HarvestParametersDTO unknown = new HarvestParametersDTO(1) {
    };
    assertThrows(IllegalArgumentException.class, () ->
        HarvestParametersConverter.convertToHarvestParametersEntity(dataset, unknown)
    );
  }

  @Test
  void convertToHarvestParametersDTO_OaiHarvestParameters() {
    OaiHarvestParametersEntity oaiHarvestParametersEntity = new OaiHarvestParametersEntity();
    oaiHarvestParametersEntity.setUrl("https://example.org");
    oaiHarvestParametersEntity.setSetSpec("set");
    oaiHarvestParametersEntity.setMetadataFormat("oai_dc");
    oaiHarvestParametersEntity.setStepSize(1);

    HarvestParametersDTO harvestParametersDTO = HarvestParametersConverter.convertToHarvestParametersDTO(
        oaiHarvestParametersEntity);
    assertInstanceOf(OaiHarvestParametersDTO.class, harvestParametersDTO);
    OaiHarvestParametersDTO oaiHarvestParametersDTO = (OaiHarvestParametersDTO) harvestParametersDTO;
    assertEquals(oaiHarvestParametersEntity.getUrl(), oaiHarvestParametersDTO.getUrl());
    assertEquals(oaiHarvestParametersEntity.getSetSpec(), oaiHarvestParametersDTO.getSetSpec());
    assertEquals(oaiHarvestParametersEntity.getMetadataFormat(), oaiHarvestParametersDTO.getMetadataFormat());
    assertEquals(oaiHarvestParametersEntity.getStepSize(), oaiHarvestParametersDTO.getStepSize());
  }

  @Test
  void convertToHarvestParametersDTO_HttpHarvestParameters() {
    HttpHarvestParametersEntity httpHarvestParameters = new HttpHarvestParametersEntity();
    httpHarvestParameters.setUrl("https://file.org");
    httpHarvestParameters.setFileName("file.zip");
    httpHarvestParameters.setFileType(FileType.ZIP);
    httpHarvestParameters.setFileContent("data".getBytes());
    httpHarvestParameters.setStepSize(1);

    HarvestParametersDTO harvestParametersDTO = HarvestParametersConverter.convertToHarvestParametersDTO(httpHarvestParameters);
    assertInstanceOf(HttpHarvestParametersDTO.class, harvestParametersDTO);
    HttpHarvestParametersDTO httpHarvestDTO = (HttpHarvestParametersDTO) harvestParametersDTO;
    assertEquals(httpHarvestParameters.getUrl(), httpHarvestDTO.getUrl());
    assertEquals(httpHarvestParameters.getFileName(), httpHarvestDTO.getFileName());
    assertEquals(httpHarvestParameters.getFileType(), httpHarvestDTO.getFileType());
    assertEquals(httpHarvestParameters.getFileContent(), httpHarvestDTO.getFileContent());
    assertEquals(httpHarvestParameters.getStepSize(), httpHarvestDTO.getStepSize());
  }

  @Test
  void convertToHarvestParametersDTO_FileHarvestParameters() {
    FileHarvestParametersEntity fileHarvestParameters = new FileHarvestParametersEntity();
    fileHarvestParameters.setFileName("file.zip");
    fileHarvestParameters.setFileType(FileType.ZIP);
    fileHarvestParameters.setFileContent("data".getBytes());
    fileHarvestParameters.setStepSize(1);

    HarvestParametersDTO harvestParametersDTO = HarvestParametersConverter.convertToHarvestParametersDTO(fileHarvestParameters);
    assertInstanceOf(FileHarvestParametersDTO.class, harvestParametersDTO);
    FileHarvestParametersDTO fileHarvestDTO = (FileHarvestParametersDTO) harvestParametersDTO;
    assertEquals(fileHarvestParameters.getFileName(), fileHarvestDTO.getFileName());
    assertEquals(fileHarvestParameters.getFileType(), fileHarvestDTO.getFileType());
    assertEquals(fileHarvestParameters.getFileContent(), fileHarvestDTO.getFileContent());
    assertEquals(fileHarvestParameters.getStepSize(), fileHarvestDTO.getStepSize());
  }

  @Test
  void convertToHarvestParametersDTO_UnknownParameters() {
    HarvestParametersEntity unknown = new HarvestParametersEntity() {
    };
    assertThrows(IllegalArgumentException.class, () ->
        HarvestParametersConverter.convertToHarvestParametersDTO(unknown)
    );
  }
}
