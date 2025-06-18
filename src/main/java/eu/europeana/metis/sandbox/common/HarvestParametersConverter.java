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
import lombok.experimental.UtilityClass;

/**
 * Utility class to convert between HarvestParametersEntity and HarvestParametersDTO.
 *
 * <p>Supports conversion for various types of harvesting parameters, including OAI-PMH, HTTP, and File harvests.
 * <p>Handles mapping of entity-specific fields and validation of supported types.
 */
@UtilityClass
public class HarvestParametersConverter {

  /**
   * Converts an instance of HarvestParametersEntity to a HarvestParametersDTO.
   *
   * <p>This method maps different subclasses of HarvestParametersEntity
   * (such as {@link OaiHarvestParameters}, {@link HttpHarvestParameters}, and {@link FileHarvestParameters}).
   *
   * @param harvestParametersEntity The HarvestParametersEntity object to be converted.
   * @return A HarvestParametersDTO object to the given HarvestParametersEntity.
   * @throws IllegalArgumentException if the provided DTO type is unsupported
   */
  public static HarvestParametersDTO convertToHarvestParametersDTO(HarvestParametersEntity harvestParametersEntity) {
    return switch (harvestParametersEntity) {
      case OaiHarvestParameters oaiHarvestParameters -> new OaiHarvestDTO(
          oaiHarvestParameters.getUrl(),
          oaiHarvestParameters.getSetSpec(),
          oaiHarvestParameters.getMetadataFormat(),
          oaiHarvestParameters.getStepSize()
      );
      case HttpHarvestParameters httpHarvestParameters -> new HttpHarvestDTO(
          httpHarvestParameters.getUrl(),
          httpHarvestParameters.getFileName(),
          httpHarvestParameters.getFileType(),
          httpHarvestParameters.getFileContent(),
          httpHarvestParameters.getStepSize()
      );
      case FileHarvestParameters fileHarvestParameters -> new FileHarvestDTO(
          fileHarvestParameters.getFileName(),
          fileHarvestParameters.getFileType(),
          fileHarvestParameters.getFileContent(),
          fileHarvestParameters.getStepSize()
      );
      default -> throw new IllegalArgumentException("Unsupported entity type: " + harvestParametersEntity.getClass());
    };
  }

  /**
   * Converts a HarvestParametersDTO to a HarvestParametersEntity based on the type of the DTO.
   *
   * @param datasetEntity The dataset entity to associate with the harvest parameters entity.
   * @param harvestParametersDTO The data transfer object containing harvest parameters.
   * @return A new instance of HarvestParametersEntity corresponding to the specific type of DTO provided.
   * @throws IllegalArgumentException if the provided DTO type is unsupported
   */
  public static HarvestParametersEntity convertToHarvestParametersEntity(
      DatasetEntity datasetEntity, HarvestParametersDTO harvestParametersDTO) {
    return switch (harvestParametersDTO) {
      case OaiHarvestDTO oaiHarvestDTO -> {
        OaiHarvestParameters oaiHarvestParameters = new OaiHarvestParameters();
        oaiHarvestParameters.setDatasetEntity(datasetEntity);
        oaiHarvestParameters.setUrl(oaiHarvestDTO.getUrl());
        oaiHarvestParameters.setSetSpec(oaiHarvestDTO.getSetSpec());
        oaiHarvestParameters.setMetadataFormat(oaiHarvestDTO.getMetadataFormat());
        oaiHarvestParameters.setStepSize(oaiHarvestDTO.getStepSize());
        yield oaiHarvestParameters;
      }
      case HttpHarvestDTO httpHarvestDTO -> {
        HttpHarvestParameters httpHarvestParameters = new HttpHarvestParameters();
        httpHarvestParameters.setDatasetEntity(datasetEntity);
        httpHarvestParameters.setUrl(httpHarvestDTO.getUrl());
        httpHarvestParameters.setStepSize(httpHarvestDTO.getStepSize());
        applyBinaryFields(httpHarvestParameters, httpHarvestDTO);
        yield httpHarvestParameters;
      }
      case FileHarvestDTO fileHarvestDTO -> {
        FileHarvestParameters fileHarvestParameters = new FileHarvestParameters();
        fileHarvestParameters.setDatasetEntity(datasetEntity);
        fileHarvestParameters.setStepSize(fileHarvestDTO.getStepSize());
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

