package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.dto.harvest.BinaryHarvestDTO;
import eu.europeana.metis.sandbox.dto.harvest.FileHarvestDTO;
import eu.europeana.metis.sandbox.dto.harvest.HarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.HttpHarvestDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.harvest.BinaryHarvestParameters;
import eu.europeana.metis.sandbox.entity.harvest.FileHarvestParameters;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HttpHarvestParameters;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParameters;

public class HarvestParametersConverter {

  public static HarvestParametersDTO convertToHarvestParametersDTO(HarvestParametersEntity harvestParametersEntity) {
    return switch (harvestParametersEntity) {
      case OaiHarvestParameters oaiHarvestParameters -> new OaiHarvestDTO(
          oaiHarvestParameters.getUrl(),
          oaiHarvestParameters.getSetSpec(),
          oaiHarvestParameters.getMetadataFormat()
      );
      case HttpHarvestParameters httpHarvestParameters -> new HttpHarvestDTO(
          httpHarvestParameters.getUrl(),
          httpHarvestParameters.getFileName(),
          httpHarvestParameters.getFileType(),
          httpHarvestParameters.getFileContent()
      );
      case FileHarvestParameters fileHarvestParameters -> new FileHarvestDTO(
          fileHarvestParameters.getFileName(),
          fileHarvestParameters.getFileType(),
          fileHarvestParameters.getFileContent()
      );
      default -> throw new IllegalArgumentException("Unsupported entity type: " + harvestParametersEntity.getClass());
    };
  }

  public static HarvestParametersEntity convertToHarvestParametersEntity(
      DatasetEntity datasetEntity, HarvestParametersDTO harvestParametersDTO) {
    return switch (harvestParametersDTO) {
      case OaiHarvestDTO oaiHarvestDTO -> {
        OaiHarvestParameters oaiHarvestParameters = new OaiHarvestParameters();
        oaiHarvestParameters.setDatasetEntity(datasetEntity);
        oaiHarvestParameters.setUrl(oaiHarvestDTO.getUrl());
        oaiHarvestParameters.setSetSpec(oaiHarvestDTO.getSetSpec());
        oaiHarvestParameters.setMetadataFormat(oaiHarvestDTO.getMetadataFormat());
        yield oaiHarvestParameters;
      }
      case HttpHarvestDTO httpHarvestDTO -> {
        HttpHarvestParameters httpHarvestParameters = new HttpHarvestParameters();
        httpHarvestParameters.setDatasetEntity(datasetEntity);
        httpHarvestParameters.setUrl(httpHarvestDTO.getUrl());
        applyBinaryFields(httpHarvestParameters, httpHarvestDTO);
        yield httpHarvestParameters;
      }
      case FileHarvestDTO fileHarvestDTO -> {
        FileHarvestParameters fileHarvestParameters = new FileHarvestParameters();
        fileHarvestParameters.setDatasetEntity(datasetEntity);
        applyBinaryFields(fileHarvestParameters, fileHarvestDTO);
        yield fileHarvestParameters;
      }
      default -> throw new IllegalArgumentException("Unsupported DTO type: " + harvestParametersDTO.getClass());
    };
  }

  private static void applyBinaryFields(BinaryHarvestParameters binaryHarvestParameters, BinaryHarvestDTO binaryHarvestDTO) {
    binaryHarvestParameters.setFileName(binaryHarvestDTO.getFileName());
    binaryHarvestParameters.setFileType(binaryHarvestDTO.getFileType());
    binaryHarvestParameters.setFileContent(binaryHarvestDTO.getFileContent());
  }
}

