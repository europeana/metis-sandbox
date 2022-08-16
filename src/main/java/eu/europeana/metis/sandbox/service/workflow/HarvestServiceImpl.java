package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.utils.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.Record.RecordBuilder;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.dataset.RecordPublishService;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HarvestServiceImpl implements HarvestService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HarvestServiceImpl.class);

  private final HttpHarvester httpHarvester;
  private final OaiHarvester oaiHarvester;
  private final RecordPublishService recordPublishService;
  private final DatasetService datasetService;
  private final int maxRecords;
  private final RecordRepository recordRepository;

  @Autowired
  public HarvestServiceImpl(HttpHarvester httpHarvester,
      OaiHarvester oaiHarvester,
      RecordPublishService recordPublishService,
      DatasetService datasetService,
      @Value("${sandbox.dataset.max-size}") int maxRecords,
      RecordRepository recordRepository) {
    this.httpHarvester = httpHarvester;
    this.recordPublishService = recordPublishService;
    this.datasetService = datasetService;
    this.recordRepository = recordRepository;
    this.oaiHarvester = oaiHarvester;
    this.maxRecords = maxRecords;
  }

  @Override
  public void harvestOaiPmh(String datasetId, Record.RecordBuilder recordDataEncapsulated, OaiHarvestData oaiHarvestData) {
    publishHarvestedRecords(harvestOaiIdentifiers(datasetId, recordDataEncapsulated, oaiHarvestData),
        datasetId,
        "Error harvesting OAI-PMH records",
        Step.HARVEST_OAI_PMH);
  }

  private List<RecordInfo> harvestOaiIdentifiers(String datasetId, Record.RecordBuilder recordDataEncapsulated,
      @NotNull OaiHarvestData oaiHarvestData) {
    List<RecordInfo> recordInfoList = new ArrayList<>();
    datasetService.updateNumberOfTotalRecord(datasetId, null);

    try (OaiRecordHeaderIterator recordHeaderIterator = oaiHarvester.harvestRecordHeaders(
        new OaiHarvest(oaiHarvestData.getUrl(),
            oaiHarvestData.getMetadataformat(),
            oaiHarvestData.getSetspec()))) {

      AtomicLong numberOfIterations = new AtomicLong();

      recordHeaderIterator.forEach(recordHeader -> {
            try {
              OaiHarvestData completeOaiHarvestData = new OaiHarvestData(oaiHarvestData.getUrl(),
                  oaiHarvestData.getSetspec(),
                  oaiHarvestData.getMetadataformat(),
                  recordHeader.getOaiIdentifier());

              if (recordHeader.isDeleted()) {
                return ReportingIteration.IterationResult.CONTINUE;
              }

              numberOfIterations.getAndIncrement();

              if (numberOfIterations.get() > maxRecords) {
                datasetService.setRecordLimitExceeded(datasetId);
                numberOfIterations.set(maxRecords);
                return ReportingIteration.IterationResult.TERMINATE;
              }

              recordInfoList.add(harvestOaiRecords(datasetId, completeOaiHarvestData, recordDataEncapsulated, recordInfoList));

            } catch (RuntimeException harvestException) {
              saveErrorWhileHarvesting(recordDataEncapsulated, recordHeader.getOaiIdentifier(), Step.HARVEST_OAI_PMH,
                  harvestException);
            }
            return ReportingIteration.IterationResult.CONTINUE;
          }
      );

      datasetService.updateNumberOfTotalRecord(datasetId, numberOfIterations.get());

    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records ", e);
    }

    return recordInfoList.stream()
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
  }

  private RecordInfo harvestOaiRecords(String datasetId, OaiHarvestData oaiHarvestData,
      Record.RecordBuilder recordToHarvest, List<RecordInfo> recordInfoList) {
    RecordInfo recordInfo;
    List<RecordError> recordErrors = new ArrayList<>();
    try {
      OaiRepository oaiRepository = new OaiRepository(oaiHarvestData.getUrl(),
          oaiHarvestData.getMetadataformat());
      OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiRepository,
          oaiHarvestData.getOaiIdentifier());
      RecordEntity recordEntity = new RecordEntity(null, oaiHarvestData.getOaiIdentifier(), datasetId, "", "");
      byte[] recordContent = oaiRecord.getRecord().readAllBytes();

      if (isDuplicatedByProviderId(recordEntity, datasetId)
          || isDuplicatedByContent(recordContent, recordInfoList)) {
        recordInfo = handleDuplicated(oaiHarvestData.getOaiIdentifier(), Step.HARVEST_OAI_PMH, recordToHarvest);
      } else {
        recordEntity = recordRepository.save(recordEntity);
        Record harvestedRecord = recordToHarvest
            .providerId(oaiHarvestData.getOaiIdentifier())
            .content(recordContent)
            .recordId(recordEntity.getId())
            .build();
        recordInfo = new RecordInfo(harvestedRecord, recordErrors);
      }

      return recordInfo;
    } catch (HarvesterException | IOException e) {
      LOGGER.error("Error harvesting OAI-PMH Record Header: {} with exception {}", oaiHarvestData.getOaiIdentifier(), e);
      saveErrorWhileHarvesting(recordToHarvest, oaiHarvestData.getOaiIdentifier(), Step.HARVEST_OAI_PMH, new RuntimeException(e));
      return null;
    }
  }

  @Override
  public void harvest(InputStream inputStream, String datasetId, Record.RecordBuilder recordDataEncapsulated)
      throws ServiceException {
    publishHarvestedRecords(harvestInputStreamIdentifiers(inputStream, datasetId, recordDataEncapsulated),
        datasetId,
        "Error harvesting file records",
        Step.HARVEST_ZIP);
  }

  private List<RecordInfo> harvestInputStreamIdentifiers(InputStream inputStream, String datasetId,
      Record.RecordBuilder recordDataEncapsulated) {
    List<Pair<Path, Exception>> exception = new ArrayList<>(1);
    List<RecordInfo> recordInfoList = new ArrayList<>();
    datasetService.updateNumberOfTotalRecord(datasetId, null);

    try {
      AtomicLong numberOfIterations = new AtomicLong(0);
      final HttpRecordIterator iterator = httpHarvester.createTemporaryHttpHarvestIterator(inputStream,
          CompressedFileExtension.ZIP);
      iterator.forEach(path -> {
        try (InputStream content = Files.newInputStream(path)) {

          numberOfIterations.getAndIncrement();

          if (numberOfIterations.get() > maxRecords) {
            datasetService.setRecordLimitExceeded(datasetId);
            numberOfIterations.set(maxRecords);
            return ReportingIteration.IterationResult.TERMINATE;
          }

          recordInfoList.add(harvestInputStream(content, datasetId, recordDataEncapsulated, path, recordInfoList));

          return ReportingIteration.IterationResult.CONTINUE;

        } catch (IOException | RuntimeException e) {
          exception.add(new ImmutablePair<>(path, e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
      });

      // Attempt to delete the temporary iterator content.
      iterator.deleteIteratorContent();

      datasetService.updateNumberOfTotalRecord(datasetId, numberOfIterations.get());

      if (!exception.isEmpty()) {
        throw new HarvesterException("Could not process path " + exception.get(0).getKey() + ".",
            exception.get(0).getValue());
      }
    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting records ", e);
    } finally {
      closeStream(inputStream);
    }

    return recordInfoList.stream()
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
  }

  private RecordInfo harvestInputStream(InputStream inputStream, String datasetId, Record.RecordBuilder recordToHarvest,
      Path path, List<RecordInfo> recordInfoList) throws ServiceException {
    List<RecordError> recordErrors = new ArrayList<>();
    RecordInfo recordInfo;
    RecordEntity recordEntity = new RecordEntity(null, path.toString(), datasetId, "", "");

    try {
      byte[] recordContent = new ByteArrayInputStream(IOUtils.toByteArray(inputStream)).readAllBytes();

      if (isDuplicatedByProviderId(recordEntity, datasetId)
          || isDuplicatedByContent(recordContent, recordInfoList)) {
        recordInfo = handleDuplicated(path.toString(), Step.HARVEST_ZIP, recordToHarvest);
      } else {
        recordEntity = recordRepository.save(recordEntity);
        Record harvestedRecord = recordToHarvest
            .providerId(path.toString())
            .content(recordContent)
            .recordId(recordEntity.getId())
            .build();
        recordInfo = new RecordInfo(harvestedRecord, recordErrors);
      }

      return recordInfo;
    } catch (RuntimeException | IOException e) {
      LOGGER.error("Error harvesting file records: {} with exception {}", recordEntity.getId(), e);
      saveErrorWhileHarvesting(recordToHarvest, path.toString(), Step.HARVEST_ZIP, new RuntimeException(e));
      return null;
    }
  }

  private void publishHarvestedRecords(final List<RecordInfo> recordInfoList,
      final String datasetId,
      final String exceptionMessage,
      final Step processStep) throws ServiceException {
    try {
      if (datasetService.isXsltPresent(datasetId)) {
        recordInfoList.parallelStream()
                      .forEach(recordInfo ->
                          recordPublishService.publishToTransformationToEdmExternalQueue(recordInfo, processStep));
      } else {
        recordInfoList.parallelStream()
                      .forEach(recordInfo ->
                          recordPublishService.publishToHarvestQueue(recordInfo, processStep));
      }
    } catch (RuntimeException e) {
      throw new ServiceException(exceptionMessage, e);
    }
  }

  private void closeStream(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        LOGGER.error("Unable to close harvest stream", e);
      }
    }
  }

  private void saveErrorWhileHarvesting(RecordBuilder recordDataEncapsulated,
      String providerIdWithError,
      Step step,
      RuntimeException harvest) {
    final String errorMessage = "Error harvesting record:";
    final String causeMessage = "Cause: " + findCause(harvest);
    RecordError recordErrorCreated = new RecordError(errorMessage + " " + providerIdWithError + " " + causeMessage,
        causeMessage);
    try {
      RecordEntity recordEntity = new RecordEntity(recordDataEncapsulated
          .providerId(providerIdWithError)
          .content((errorMessage + providerIdWithError).getBytes(StandardCharsets.UTF_8))
          .build());

      RecordLogEntity recordLogEntity = new RecordLogEntity(recordEntity,
          providerIdWithError, step,
          Status.FAIL);

      RecordErrorLogEntity recordErrorLogEntity = new RecordErrorLogEntity(recordEntity, step, Status.FAIL,
          recordErrorCreated.getMessage(), recordErrorCreated.getStackTrace());
      recordEntity.setRecordLogEntity(List.of(recordLogEntity));
      recordEntity.setRecordErrorLogEntity(List.of(recordErrorLogEntity));

      recordRepository.save(recordEntity);
    } catch (RuntimeException ex) {
      LOGGER.error("Unable to handle error log while harvesting.", ex);
    }
  }

  private String findCause(Throwable throwable) {
    if (throwable.getCause() == null) {
      return throwable.getMessage();
    } else {
      return findCause(throwable.getCause());
    }
  }

  private boolean isDuplicatedByProviderId(RecordEntity recordEntity, String datasetId) {
    RecordEntity recordFound = recordRepository.findByProviderIdAndDatasetId(recordEntity.getProviderId(), datasetId);
    return recordFound != null;
  }

  private boolean isDuplicatedByContent(byte[] content, List<RecordInfo> recordInfoList) {
    Optional<RecordInfo> optionalRecordInfo = recordInfoList.stream()
                                                            .filter(Objects::nonNull)
                                                            .filter(
                                                                recordInfo -> Arrays.equals(
                                                                    recordInfo.getRecord()
                                                                              .getContent(),
                                                                    content))
                                                            .findFirst();
    return optionalRecordInfo.isPresent();
  }

  private RecordInfo handleDuplicated(String providerId, Step step, Record.RecordBuilder recordToHarvest) {
    RecordError recordErrorCreated = new RecordError("Duplicated record", "Record already registered");
    saveErrorWhileHarvesting(recordToHarvest, providerId, step, new RuntimeException(recordErrorCreated.getMessage()));
    return null;
  }
}
