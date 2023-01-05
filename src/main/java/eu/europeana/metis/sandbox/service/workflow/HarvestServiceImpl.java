package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.harvesting.oaipmh.*;
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
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
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

  private static final int NUMBER_OF_RECORDS_TO_STEP_INTO = 5;

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

    try (OaiRecordHeaderIterator recordHeaderIterator = oaiHarvester.harvestRecordHeaders(
        new OaiHarvest(oaiHarvestData.getUrl(),
            oaiHarvestData.getMetadataformat(),
            oaiHarvestData.getSetspec()))) {

      List<OaiRecordHeader> filteredIterator = filterHeaders(recordHeaderIterator, datasetId);

      filteredIterator.forEach(recordHeader -> {
            try {
              OaiHarvestData completeOaiHarvestData = new OaiHarvestData(oaiHarvestData.getUrl(),
                  oaiHarvestData.getSetspec(),
                  oaiHarvestData.getMetadataformat(),
                  recordHeader.getOaiIdentifier());

              recordInfoList.add(harvestOaiRecords(datasetId, completeOaiHarvestData, recordDataEncapsulated));

            } catch (RuntimeException harvestException) {
              saveErrorWhileHarvesting(recordDataEncapsulated, recordHeader.getOaiIdentifier(), Step.HARVEST_OAI_PMH,
                  harvestException);
            }
          }
      );
      //TODO: MET-4888 This method currently causes no race condition issues. But if harvesting is to ever happen
      //TODO: through multiple nodes, then a race condition will surface because of the method bellow.
      datasetService.updateNumberOfTotalRecord(datasetId, (long) filteredIterator.size());

    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records ", e);
    }

    return recordInfoList.stream()
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
  }

  private List<OaiRecordHeader> filterHeaders(OaiRecordHeaderIterator iteratorToFilter, String datasetId) throws HarvesterException {

    List<OaiRecordHeader> result = new ArrayList<>();

    AtomicInteger numberOfSelectedHeaders = new AtomicInteger();
    AtomicInteger currentIndex = new AtomicInteger();
    AtomicInteger nextIndexToSelect = new AtomicInteger();

    iteratorToFilter.forEach(oaiRecordHeader -> {
      if(numberOfSelectedHeaders.get() >= maxRecords){
        //TODO: MET-4888 This method currently causes no race condition issues. But if harvesting is to ever happen
        //TODO: through multiple nodes, then a race condition will surface because of the method bellow.
        datasetService.setRecordLimitExceeded(datasetId);
        return ReportingIteration.IterationResult.TERMINATE;
      }

      if(currentIndex.get() > maxRecords){
        // This indicates that the dataset has more than max number of records,
        // but the number of selected records has not reached max, therefore it continues
        datasetService.setRecordLimitExceeded(datasetId);
      }

      if(currentIndex.get() == nextIndexToSelect.get()){
        if(oaiRecordHeader.isDeleted()){
          nextIndexToSelect.getAndIncrement();
        } else {
          result.add(oaiRecordHeader);
          nextIndexToSelect.addAndGet(NUMBER_OF_RECORDS_TO_STEP_INTO);
          numberOfSelectedHeaders.getAndIncrement();
        }
      }
      currentIndex.getAndIncrement();
      return ReportingIteration.IterationResult.CONTINUE;
    });

    return result;
  }

  private RecordInfo harvestOaiRecords(String datasetId, OaiHarvestData oaiHarvestData, Record.RecordBuilder recordToHarvest) {
    RecordInfo recordInfo;
    List<RecordError> recordErrors = new ArrayList<>();
    try {
      OaiRepository oaiRepository = new OaiRepository(oaiHarvestData.getUrl(),
          oaiHarvestData.getMetadataformat());
      OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiRepository,
          oaiHarvestData.getOaiIdentifier());
      RecordEntity recordEntity = new RecordEntity(null, oaiHarvestData.getOaiIdentifier(), datasetId, "", "");
      byte[] recordContent = oaiRecord.getRecord().readAllBytes();

      recordEntity = recordRepository.save(recordEntity);
      Record harvestedRecord = recordToHarvest
              .providerId(oaiHarvestData.getOaiIdentifier())
              .content(recordContent)
              .recordId(recordEntity.getId())
              .build();
        recordInfo = new RecordInfo(harvestedRecord, recordErrors);

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

    try {
      AtomicInteger numberOfSelectedHeaders = new AtomicInteger();
      AtomicInteger currentIndex = new AtomicInteger();
      AtomicInteger nextIndexToSelect = new AtomicInteger();

      final HttpRecordIterator iterator = httpHarvester.createTemporaryHttpHarvestIterator(inputStream,
          CompressedFileExtension.ZIP);
      final String extractedDirectoryFromIterator = iterator.getExtractedDirectory();
      iterator.forEach(path -> {
        try (InputStream content = Files.newInputStream(path)) {

          if (numberOfSelectedHeaders.get() >= maxRecords) {
            //TODO: MET-4888 This method currently causes no race condition issues. But if harvesting is to ever happen
            //TODO: through multiple nodes, then a race condition will surface because of the method bellow.
            datasetService.setRecordLimitExceeded(datasetId);
            numberOfSelectedHeaders.set(maxRecords);
            return ReportingIteration.IterationResult.TERMINATE;
          }

          if(currentIndex.get() == nextIndexToSelect.get()){
            recordInfoList.add(harvestInputStream(content, datasetId, recordDataEncapsulated, path, extractedDirectoryFromIterator));
            nextIndexToSelect.addAndGet(NUMBER_OF_RECORDS_TO_STEP_INTO);
            numberOfSelectedHeaders.incrementAndGet();
          }

          currentIndex.incrementAndGet();
          return ReportingIteration.IterationResult.CONTINUE;

        } catch (IOException | RuntimeException e) {
          exception.add(new ImmutablePair<>(path, e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
      });

      // Attempt to delete the temporary iterator content.
      iterator.deleteIteratorContent();

      //TODO: MET-4888 This method currently causes no race condition issues. But if harvesting is to ever happen
      //TODO: through multiple nodes, then a race condition will surface because of the method bellow.
      datasetService.updateNumberOfTotalRecord(datasetId, (long) numberOfSelectedHeaders.get());

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
      Path path, String extractedDirectoryFromIterator) throws ServiceException {
    List<RecordError> recordErrors = new ArrayList<>();
    RecordInfo recordInfo;
    String tmpProviderId = createTemporaryIdFromPath(extractedDirectoryFromIterator, path);
    RecordEntity recordEntity = new RecordEntity(null, tmpProviderId, datasetId, "", "");

    try {
      byte[] recordContent = new ByteArrayInputStream(IOUtils.toByteArray(inputStream)).readAllBytes();

      recordEntity = recordRepository.save(recordEntity);
      Record harvestedRecord = recordToHarvest
              .providerId(tmpProviderId)
              .content(recordContent)
              .recordId(recordEntity.getId())
              .build();
      recordInfo = new RecordInfo(harvestedRecord, recordErrors);

      return recordInfo;
    } catch (RuntimeException | IOException e) {
      LOGGER.error("Error harvesting file records: {} with exception {}", recordEntity.getId(), e);
      saveErrorWhileHarvesting(recordToHarvest, tmpProviderId, Step.HARVEST_ZIP, new RuntimeException(e));
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

  private String createTemporaryIdFromPath(String extractedDirectory, Path pathToRelativize){
    return extractedDirectory.isBlank() ? pathToRelativize.getFileName().toString() : 
            Paths.get(extractedDirectory).relativize(pathToRelativize).toString();

  }
}
