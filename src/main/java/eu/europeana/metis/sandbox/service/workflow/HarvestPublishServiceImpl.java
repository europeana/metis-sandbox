package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.dto.FileHarvestingDto;
import eu.europeana.metis.sandbox.dto.HttpHarvestingDto;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestingDto;
import eu.europeana.metis.sandbox.service.dataset.HarvestingParameterService;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestPublishServiceImpl implements HarvestPublishService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HarvestPublishServiceImpl.class);
  private static final String HARVESTING_ERROR_MESSAGE = "Error harvesting records for dataset: ";

  private final HarvestService harvestService;
  private final Executor asyncServiceTaskExecutor;
  private final HarvestingParameterService harvestingParameterService;

  public HarvestPublishServiceImpl(HarvestService harvestService,
      @Qualifier("asyncServiceTaskExecutor") Executor asyncServiceTaskExecutor,
      HarvestingParameterService harvestingParameterService) {
    this.harvestService = harvestService;
    this.asyncServiceTaskExecutor = asyncServiceTaskExecutor;
    this.harvestingParameterService = harvestingParameterService;
  }

  @Override
  public CompletableFuture<Void> runHarvestProvidedFileAsync(MultipartFile file, DatasetMetadata datasetMetadata,
      CompressedFileExtension compressedFileExtension) {
    try {
      harvestingParameterService.createDatasetHarvestingParameters(datasetMetadata.getDatasetId(),
          new FileHarvestingDto(file.getOriginalFilename(), compressedFileExtension.name()));
      return runHarvestFileAsync(file.getInputStream(), datasetMetadata, compressedFileExtension);
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from file " + file.getName(), e);
    }
  }

  @Override
  public CompletableFuture<Void> runHarvestHttpFileAsync(String url, DatasetMetadata datasetMetadata,
      CompressedFileExtension compressedFileExtension) {
    harvestingParameterService.createDatasetHarvestingParameters(datasetMetadata.getDatasetId(), new HttpHarvestingDto(url));
    try (InputStream inputStreamToHarvest = new URI(url).toURL().openStream()) {
      return runHarvestFileAsync(inputStreamToHarvest, datasetMetadata, compressedFileExtension);
    } catch (UnknownHostException e) {
      throw new ServiceException(HARVESTING_ERROR_MESSAGE + datasetMetadata.getDatasetId()
          + " - unknown host: " + e.getMessage());
    } catch (IOException | URISyntaxException e) {
      throw new ServiceException(HARVESTING_ERROR_MESSAGE + datasetMetadata.getDatasetId(), e);
    }
  }

  private CompletableFuture<Void> runHarvestFileAsync(InputStream inputStreamToHarvest,
      DatasetMetadata datasetMetadata, CompressedFileExtension compressedFileExtension) {
    return CompletableFuture.runAsync(() -> {
      final Record.RecordBuilder recordDataEncapsulated = Record.builder()
          .datasetId(datasetMetadata.getDatasetId())
          .datasetName(datasetMetadata.getDatasetName())
          .country(datasetMetadata.getCountry())
          .language(datasetMetadata.getLanguage());
      try {
        harvestService.harvestFromCompressedArchive(inputStreamToHarvest,
            datasetMetadata.getDatasetId(), recordDataEncapsulated,
            datasetMetadata.getStepSize(), compressedFileExtension);
      } catch (HarvesterException e) {
        throw new ServiceException(HARVESTING_ERROR_MESSAGE + datasetMetadata.getDatasetId(), e);
      } finally {
        try {
          inputStreamToHarvest.close();
        } catch (IOException e) {
          LOGGER.warn("Could not close input stream", e);
        }
      }
    }, asyncServiceTaskExecutor);
  }

  @Override
  public CompletableFuture<Void> runHarvestOaiPmhAsync(DatasetMetadata datasetMetadata,
      OaiHarvestData oaiHarvestData) {
    Record.RecordBuilder recordDataEncapsulated = Record.builder()
                                                        .country(datasetMetadata.getCountry())
                                                        .language(datasetMetadata.getLanguage())
                                                        .datasetName(datasetMetadata.getDatasetName())
                                                        .datasetId(datasetMetadata.getDatasetId());
    harvestingParameterService.createDatasetHarvestingParameters(datasetMetadata.getDatasetId(),
        new OAIPmhHarvestingDto(oaiHarvestData.getUrl(), oaiHarvestData.getSetspec(),
            oaiHarvestData.getMetadataformat()));
    return CompletableFuture.runAsync(
        () -> harvestService.harvestFromOaiPmh(datasetMetadata.getDatasetId(), recordDataEncapsulated, oaiHarvestData,
            datasetMetadata.getStepSize()), asyncServiceTaskExecutor);
  }
}
