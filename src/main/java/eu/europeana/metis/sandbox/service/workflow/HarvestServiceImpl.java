package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.FullRecord;
import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.exception.StepIsTooBigException;
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
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
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
  private static final int DEFAULT_STEP_SIZE = 1;

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
  public void harvestFromOaiPmh(String datasetId, RecordBuilder recordDataEncapsulated, OaiHarvestData oaiHarvestData, Integer stepSize) {
    publishHarvestedRecords(harvestOaiIdentifiers(datasetId, recordDataEncapsulated, oaiHarvestData, stepSize),
        datasetId,
        "Error harvesting OAI-PMH records",
        Step.HARVEST_OAI_PMH);
  }

  private List<RecordInfo> harvestOaiIdentifiers(String datasetId, Record.RecordBuilder recordDataEncapsulated,
      @NotNull OaiHarvestData oaiHarvestData, Integer stepSize) {
    List<RecordInfo> recordInfoList = new ArrayList<>();

    try (HarvestingIterator<OaiRecordHeader, OaiRecordHeader> recordHeaderIterator = oaiHarvester.harvestRecordHeaders(
        new OaiHarvest(oaiHarvestData.getUrl(),
            oaiHarvestData.getMetadataformat(),
            oaiHarvestData.getSetspec()))) {

      List<OaiRecordHeader> filteredIterator = harvestOaiHeaders(recordHeaderIterator, datasetId, stepSize);

      if(filteredIterator.isEmpty()){
        return Collections.emptyList();
      }

      filteredIterator.forEach(recordHeader -> {
            try {
              OaiHarvestData completeOaiHarvestData = new OaiHarvestData(oaiHarvestData.getUrl(),
                  oaiHarvestData.getSetspec(),
                  oaiHarvestData.getMetadataformat(),
                  recordHeader.getOaiIdentifier());

              recordInfoList.add(
                  harvestOaiRecord(datasetId, completeOaiHarvestData, recordDataEncapsulated));

            } catch (RuntimeException harvestException) {
              saveErrorWhileHarvesting(recordDataEncapsulated, recordHeader.getOaiIdentifier(),
                      Step.HARVEST_OAI_PMH,
                  harvestException);
            }
          }
      );

    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records ", e);
    }

    return recordInfoList.stream()
                         .filter(Objects::nonNull)
                         .toList();
  }

  private List<OaiRecordHeader> harvestOaiHeaders(HarvestingIterator<OaiRecordHeader,
      OaiRecordHeader> iteratorToFilter, String datasetId, Integer stepSize) throws HarvesterException {
    final List<OaiRecordHeader> result = new ArrayList<>();
    harvestFromIterator(iteratorToFilter, datasetId, stepSize, entry -> {
      result.add(entry);
      return ReportingIteration.IterationResult.CONTINUE;
    }, OaiRecordHeader::isDeleted);
    return result;
  }

  private RecordInfo harvestOaiRecord(String datasetId, OaiHarvestData oaiHarvestData, Record.RecordBuilder recordToHarvest) {
    RecordInfo recordInfo;
    List<RecordError> recordErrors = new ArrayList<>();
    try {
      OaiRepository oaiRepository = new OaiRepository(oaiHarvestData.getUrl(),
          oaiHarvestData.getMetadataformat());
      OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiRepository,
          oaiHarvestData.getOaiIdentifier());
      RecordEntity recordEntity = new RecordEntity(oaiHarvestData.getOaiIdentifier(), datasetId);
      byte[] recordContent = oaiRecord.getContent().readAllBytes();

      recordEntity = recordRepository.save(recordEntity);
      Record harvestedRecord = recordToHarvest
              .providerId(oaiHarvestData.getOaiIdentifier())
              .content(recordContent)
              .recordId(recordEntity.getId())
              .build();
        recordInfo = new RecordInfo(harvestedRecord, recordErrors);

      return recordInfo;
    } catch (HarvesterException e) {
      LOGGER.error("Error harvesting OAI-PMH Record Header: {} with exception {}", oaiHarvestData.getOaiIdentifier(), e);
      saveErrorWhileHarvesting(recordToHarvest, oaiHarvestData.getOaiIdentifier(),
              Step.HARVEST_OAI_PMH, new RuntimeException(e));
      return null;
    }
  }

  @Override
  public void harvestFromCompressedArchive(InputStream inputStream, String datasetId,
      RecordBuilder recordDataEncapsulated, Integer stepSize,
      CompressedFileExtension compressedFileExtension) throws ServiceException {

    final List<Pair<String, Exception>> exception = new ArrayList<>(1);
    final List<RecordInfo> recordInfoList = new ArrayList<>();
    try (final HarvestingIterator<FullRecord, Path> iterator = httpHarvester.createFullRecordHarvestIterator(inputStream,
        compressedFileExtension)) {

      harvestFromIterator(iterator, datasetId, stepSize, entry -> {
        try (final InputStream content = entry.getContent()) {
          recordInfoList.add(harvestRecordFromInputStream(content, datasetId, recordDataEncapsulated,
              entry.getHarvestingIdentifier()));
          return ReportingIteration.IterationResult.CONTINUE;
        } catch (IOException | RuntimeException e) {
          exception.add(new ImmutablePair<>(entry.getHarvestingIdentifier(), e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
      }, FullRecord::isDeleted);

      if (!exception.isEmpty()) {
        throw new HarvesterException("Could not process path " + exception.get(0).getKey() + ".",
            exception.get(0).getValue());
      }
    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting records ", e);
    }

    final List<RecordInfo> result = recordInfoList.stream().filter(Objects::nonNull).toList();
    publishHarvestedRecords(result, datasetId, "Error harvesting file records", Step.HARVEST_FILE);
  }

  private RecordInfo harvestRecordFromInputStream(InputStream inputStream, String datasetId,
      Record.RecordBuilder recordToHarvest, String tmpProviderId) throws ServiceException {
    RecordInfo recordInfo;
    RecordEntity recordEntity = new RecordEntity(tmpProviderId, datasetId);

    try {
      recordEntity = recordRepository.save(recordEntity);
      Record harvestedRecord = recordToHarvest
              .providerId(tmpProviderId)
              .content(IOUtils.toByteArray(inputStream))
              .recordId(recordEntity.getId())
              .build();
      recordInfo = new RecordInfo(harvestedRecord, new ArrayList<>());

      return recordInfo;
    } catch (RuntimeException | IOException e) {
      LOGGER.error("Error harvesting file records: {} with exception {}", recordEntity.getId(), e);
      saveErrorWhileHarvesting(recordToHarvest, tmpProviderId, Step.HARVEST_FILE, new RuntimeException(e));
      return null;
    }
  }

  private <T> void harvestFromIterator(HarvestingIterator<T, ?> iterator, String datasetId,
      Integer stepSize, Function<T, ReportingIteration.IterationResult> processor,
      Predicate<T> isDeleted) throws HarvesterException {

    final int numberOfRecordsToStepInto = stepSize == null ? DEFAULT_STEP_SIZE : stepSize;
    final AtomicInteger numberOfSelectedHeaders = new AtomicInteger();
    final AtomicInteger currentIndex = new AtomicInteger();
    final AtomicInteger nextIndexToSelect = new AtomicInteger(numberOfRecordsToStepInto - 1);

    iterator.forEach(entry -> {
      if (numberOfSelectedHeaders.get() >= maxRecords) {
        //TODO: MET-4888 This method currently causes no race condition issues. But if harvesting is to ever happen
        //TODO: through multiple nodes, then a race condition will surface because of the method bellow.
        datasetService.setRecordLimitExceeded(datasetId);
        numberOfSelectedHeaders.set(maxRecords);
        return ReportingIteration.IterationResult.TERMINATE;
      }

      ReportingIteration.IterationResult result = null;
      if (currentIndex.get() == nextIndexToSelect.get()) {
        if (isDeleted.test(entry)) {
          nextIndexToSelect.getAndIncrement();
        } else {
          result = processor.apply(entry);
          nextIndexToSelect.addAndGet(numberOfRecordsToStepInto);
          numberOfSelectedHeaders.getAndIncrement();
        }
      }
      currentIndex.getAndIncrement();
      return Optional.ofNullable(result).orElse(ReportingIteration.IterationResult.CONTINUE);
    });

    //TODO: MET-4888 This method currently causes no race condition issues. But if harvesting is to ever happen
    //TODO: through multiple nodes, then a race condition will surface because of the method bellow.
    datasetService.updateNumberOfTotalRecord(datasetId, (long) numberOfSelectedHeaders.get());

    if (isStepSizeBiggerThanDatasetSize(numberOfSelectedHeaders.get(), currentIndex.get(),
        nextIndexToSelect.get(), numberOfRecordsToStepInto)) {
      throw new StepIsTooBigException(currentIndex.get());
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

  private void saveErrorWhileHarvesting(RecordBuilder recordDataEncapsulated,
      String providerIdWithError,
      Step step,
      RuntimeException harvest) {
    final String errorMessage = "Error while harvesting ";
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

  private boolean isStepSizeBiggerThanDatasetSize(int datasetSize, int currentIndex, int nextIndexToSelect, int stepSize){
    return datasetSize == 0 && currentIndex > 0 && currentIndex <= nextIndexToSelect && nextIndexToSelect < stepSize;
  }
}
