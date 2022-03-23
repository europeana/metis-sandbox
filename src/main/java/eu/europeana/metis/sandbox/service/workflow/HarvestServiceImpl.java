package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
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
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.dataset.RecordPublishService;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
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
  private final RecordErrorLogRepository recordErrorLogRepository;

  @Autowired
  public HarvestServiceImpl(HttpHarvester httpHarvester,
                            OaiHarvester oaiHarvester,
                            RecordPublishService recordPublishService,
                            DatasetService datasetService,
                            @Value("${sandbox.dataset.max-size}") int maxRecords,
                            RecordRepository recordRepository,
                            RecordErrorLogRepository recordErrorLogRepository) {
    this.httpHarvester = httpHarvester;
    this.recordPublishService = recordPublishService;
    this.datasetService = datasetService;
    this.recordRepository = recordRepository;
    this.oaiHarvester = oaiHarvester;
    this.maxRecords = maxRecords;
    this.recordErrorLogRepository = recordErrorLogRepository;
  }

  @Override
  public void harvestOaiPmh(String datasetId, Record.RecordBuilder recordDataEncapsulated, OaiHarvestData oaiHarvestData) {
    publishHarvestedRecords(harvestOaiIdentifiers(datasetId, recordDataEncapsulated, oaiHarvestData),
        datasetId,
        "Error harvesting OAI-PMH records",
        Step.HARVEST_OAI_PMH);
  }

  private List<RecordInfo> harvestOaiIdentifiers(String datasetId, Record.RecordBuilder recordDataEncapsulated,
      OaiHarvestData oaiHarvestData) {
    List<RecordInfo> recordInfoList = new ArrayList<>();
    datasetService.updateNumberOfTotalRecord(datasetId, null);

    try (OaiRecordHeaderIterator recordHeaderIterator = oaiHarvester.harvestRecordHeaders(
        new OaiHarvest(oaiHarvestData.getUrl(),
            oaiHarvestData.getMetadataformat(),
            oaiHarvestData.getSetspec()))) {

      AtomicLong currentNumberOfIterations = new AtomicLong();

      recordHeaderIterator.forEach(recordHeader -> {
        OaiHarvestData completeOaiHarvestData = new OaiHarvestData(oaiHarvestData.getUrl(),
            oaiHarvestData.getSetspec(),
            oaiHarvestData.getMetadataformat(),
            recordHeader.getOaiIdentifier());

        if (recordHeader.isDeleted()) {
          return ReportingIteration.IterationResult.CONTINUE;
        }

        currentNumberOfIterations.getAndIncrement();

        if (currentNumberOfIterations.get() > maxRecords) {
          datasetService.setRecordLimitExceeded(datasetId);
          currentNumberOfIterations.set(maxRecords);
          return ReportingIteration.IterationResult.TERMINATE;
        }

        recordInfoList.add(harvestOaiRecords(datasetId, completeOaiHarvestData,
            recordDataEncapsulated));

        return ReportingIteration.IterationResult.CONTINUE;
      });

      datasetService.updateNumberOfTotalRecord(datasetId, currentNumberOfIterations.get());

    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records ", e);
    }
    return recordInfoList;
  }

  private RecordInfo harvestOaiRecords(String datasetId, OaiHarvestData oaiHarvestData,
      Record.RecordBuilder recordToHarvest) {

    List<RecordError> recordErrors = new ArrayList<>();
    try {
      OaiRepository oaiRepository = new OaiRepository(oaiHarvestData.getUrl(),
          oaiHarvestData.getMetadataformat());
      OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiRepository,
          oaiHarvestData.getOaiIdentifier());
      RecordEntity recordEntity = recordRepository.save(
          new RecordEntity(null, oaiHarvestData.getOaiIdentifier(), datasetId));
      Record harvestedRecord = recordToHarvest
          .providerId(oaiHarvestData.getOaiIdentifier())
          .content(oaiRecord.getRecord().readAllBytes())
          .recordId(recordEntity.getId())
          .build();
      return new RecordInfo(harvestedRecord, recordErrors);

    } catch (HarvesterException | IOException e) {
      LOGGER.error("Error harvesting OAI-PMH Record Header: {} with exception {}",
          oaiHarvestData.getOaiIdentifier(), e);
      RecordError recordErrorCreated = new RecordError(
          "Error harvesting OAI-PMH Record Header:" + oaiHarvestData.getOaiIdentifier(),
          e.getMessage());
      recordErrors.add(recordErrorCreated);
      recordErrorLogRepository.save(
          new RecordErrorLogEntity(new RecordEntity(recordToHarvest.build()), Step.HARVEST_OAI_PMH, Status.FAIL,
              recordErrorCreated.getMessage(), recordErrorCreated.getStackTrace()));
      return new RecordInfo(recordToHarvest.build(), recordErrors);
    }
  }

  @Override
  public void harvest(InputStream inputStream, String datasetId, Record.RecordBuilder recordDataEncapsulated)
      throws HarvesterException {
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

          recordInfoList.add(harvestInputStream(content, datasetId, recordDataEncapsulated, path));

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

    return recordInfoList;
  }

  private RecordInfo harvestInputStream(InputStream inputStream, String datasetId, Record.RecordBuilder recordToHarvest, Path path)
      throws ServiceException {
    List<RecordError> recordErrors = new ArrayList<>();
    RecordEntity recordEntity = recordRepository.save(
        new RecordEntity(null, path.toString(), datasetId));

    try {
      Record harvestedRecord = recordToHarvest
          .providerId(path.toString())
          .content(new ByteArrayInputStream(IOUtils.toByteArray(inputStream)).readAllBytes())
          .recordId(recordEntity.getId())
          .build();

      return new RecordInfo(harvestedRecord, recordErrors);

    } catch (RuntimeException | IOException e) {
      LOGGER.error("Error harvesting file records: {} with exception {}",
          recordEntity.getId(), e);
      RecordError recordErrorCreated = new RecordError(
          "Error harvesting file records:" + recordEntity.getId(),
          e.getMessage());
      recordErrors.add(recordErrorCreated);
      recordErrorLogRepository.save(new RecordErrorLogEntity(recordEntity, Step.HARVEST_ZIP, Status.FAIL,
          recordErrorCreated.getMessage(), recordErrorCreated.getStackTrace()));

      return new RecordInfo(recordToHarvest.build(), recordErrors);
    }
  }

  private void publishHarvestedRecords(final List<RecordInfo> recordInfoList,
      final String datasetId,
      final String exceptionMessage,
      final Step processStep) {
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
}
