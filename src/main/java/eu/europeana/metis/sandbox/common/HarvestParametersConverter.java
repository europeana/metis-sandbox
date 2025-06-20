package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.dto.harvest.AbstractBinaryHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.FileHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.AbstractHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.HttpHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.harvest.AbstractBinaryHarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.FileHarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HttpHarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParametersEntity;
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
   * (such as {@link OaiHarvestParametersEntity}, {@link HttpHarvestParametersEntity}, and {@link FileHarvestParametersEntity}).
   *
   * @param harvestParametersEntity The HarvestParametersEntity object to be converted.
   * @return A HarvestParametersDTO object to the given HarvestParametersEntity.
   * @throws IllegalArgumentException if the provided DTO type is unsupported
   */
  public static AbstractHarvestParametersDTO convertToHarvestParametersDTO(HarvestParametersEntity harvestParametersEntity) {
    return switch (harvestParametersEntity) {
      case OaiHarvestParametersEntity oaiHarvestParametersEntity -> new OaiHarvestParametersDTO(
          oaiHarvestParametersEntity.getUrl(),
          oaiHarvestParametersEntity.getSetSpec(),
          oaiHarvestParametersEntity.getMetadataFormat(),
          oaiHarvestParametersEntity.getStepSize()
      );
      case HttpHarvestParametersEntity httpHarvestParameters -> new HttpHarvestParametersDTO(
          httpHarvestParameters.getUrl(),
          httpHarvestParameters.getFileName(),
          httpHarvestParameters.getFileType(),
          httpHarvestParameters.getFileContent(),
          httpHarvestParameters.getStepSize()
      );
      case FileHarvestParametersEntity fileHarvestParameters -> new FileHarvestParametersDTO(
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
   * @param abstractHarvestParametersDTO The data transfer object containing harvest parameters.
   * @return A new instance of HarvestParametersEntity corresponding to the specific type of DTO provided.
   * @throws IllegalArgumentException if the provided DTO type is unsupported
   */
  public static HarvestParametersEntity convertToHarvestParametersEntity(
      DatasetEntity datasetEntity, AbstractHarvestParametersDTO abstractHarvestParametersDTO) {
    return switch (abstractHarvestParametersDTO) {
      case OaiHarvestParametersDTO oaiHarvestParametersDTO -> {
        OaiHarvestParametersEntity oaiHarvestParametersEntity = new OaiHarvestParametersEntity();
        oaiHarvestParametersEntity.setDatasetEntity(datasetEntity);
        oaiHarvestParametersEntity.setUrl(oaiHarvestParametersDTO.getUrl());
        oaiHarvestParametersEntity.setSetSpec(oaiHarvestParametersDTO.getSetSpec());
        oaiHarvestParametersEntity.setMetadataFormat(oaiHarvestParametersDTO.getMetadataFormat());
        oaiHarvestParametersEntity.setStepSize(oaiHarvestParametersDTO.getStepSize());
        yield oaiHarvestParametersEntity;
      }
      case HttpHarvestParametersDTO httpHarvestDTO -> {
        HttpHarvestParametersEntity httpHarvestParameters = new HttpHarvestParametersEntity();
        httpHarvestParameters.setDatasetEntity(datasetEntity);
        httpHarvestParameters.setUrl(httpHarvestDTO.getUrl());
        httpHarvestParameters.setStepSize(httpHarvestDTO.getStepSize());
        applyBinaryFields(httpHarvestParameters, httpHarvestDTO);
        yield httpHarvestParameters;
      }
      case FileHarvestParametersDTO fileHarvestDTO -> {
        FileHarvestParametersEntity fileHarvestParameters = new FileHarvestParametersEntity();
        fileHarvestParameters.setDatasetEntity(datasetEntity);
        fileHarvestParameters.setStepSize(fileHarvestDTO.getStepSize());
        applyBinaryFields(fileHarvestParameters, fileHarvestDTO);
        yield fileHarvestParameters;
      }
      default -> throw new IllegalArgumentException("Unsupported DTO type: " + abstractHarvestParametersDTO.getClass());
    };
  }

  private static void applyBinaryFields(AbstractBinaryHarvestParametersEntity abstractBinaryHarvestParametersEntity,
      AbstractBinaryHarvestParametersDTO abstractBinaryHarvestParametersDTO) {
    abstractBinaryHarvestParametersEntity.setFileName(abstractBinaryHarvestParametersDTO.getFileName());
    abstractBinaryHarvestParametersEntity.setFileType(abstractBinaryHarvestParametersDTO.getFileType());
    abstractBinaryHarvestParametersEntity.setFileContent(abstractBinaryHarvestParametersDTO.getFileContent());
  }
}

