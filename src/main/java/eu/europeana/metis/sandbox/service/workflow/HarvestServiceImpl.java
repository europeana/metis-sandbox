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
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.RecordEntity;
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
import java.util.concurrent.atomic.AtomicInteger;
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
    try (OaiRecordHeaderIterator recordHeaderIterator = oaiHarvester.harvestRecordHeaders(
        new OaiHarvest(oaiHarvestData.getUrl(),
            oaiHarvestData.getMetadataformat(),
            oaiHarvestData.getSetspec()))) {

      AtomicInteger currentNumberOfIterations = new AtomicInteger();

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

        if (datasetService.isXsltPresent(datasetId)) {
          recordPublishService.publishToTransformationToEdmExternalQueue(
              harvestOaiRecordHeader(datasetId, completeOaiHarvestData,
                  recordDataEncapsulated, recordHeader.getOaiIdentifier()), Step.HARVEST_OAI_PMH);
        } else {
          recordPublishService.publishToHarvestQueue(
              harvestOaiRecordHeader(datasetId, completeOaiHarvestData,
                  recordDataEncapsulated, recordHeader.getOaiIdentifier()), Step.HARVEST_OAI_PMH);
        }

        return ReportingIteration.IterationResult.CONTINUE;
      });

      datasetService.updateNumberOfTotalRecord(datasetId, currentNumberOfIterations.get());

    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records ", e);
    }
  }

  private RecordInfo harvestOaiRecordHeader(String datasetId, OaiHarvestData oaiHarvestData,
      Record.RecordBuilder recordToHarvest, String oaiIdentifier) {

    List<RecordError> recordErrors = new ArrayList<>();
    try {
      OaiRepository oaiRepository = new OaiRepository(oaiHarvestData.getUrl(),
          oaiHarvestData.getMetadataformat());
      OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiRepository,
          oaiHarvestData.getOaiIdentifier());
      RecordEntity recordEntity = recordRepository.save(
          new RecordEntity(null, null, datasetId));
      Record harvestedRecord = recordToHarvest
          .providerId(oaiIdentifier)
          .content(oaiRecord.getRecord().readAllBytes())
          .recordId(recordEntity.getId())
          .build();
      return new RecordInfo(harvestedRecord, recordErrors);

    } catch (HarvesterException | IOException e) {
      LOGGER.error("Error harvesting OAI-PMH Record Header: {} with exception {}",
          oaiHarvestData.getOaiIdentifier(), e);
      recordErrors.add(new RecordError(
          "Error harvesting OAI-PMH Record Header:" + oaiHarvestData.getOaiIdentifier(),
          e.getMessage()));

      return new RecordInfo(recordToHarvest.build(), recordErrors);
    }
  }

  @Override
  public void harvest(InputStream inputStream, String datasetId, Record.RecordBuilder recordDataEncapsulated)
      throws HarvesterException {

    AtomicInteger numberOfIterations = new AtomicInteger(0);
    List<Pair<Path, Exception>> exception = new ArrayList<>(1);

    try {
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

          if (datasetService.isXsltPresent(datasetId)) {
            recordPublishService.publishToTransformationToEdmExternalQueue(
                harvestInputStream(content, datasetId, recordDataEncapsulated, path), Step.HARVEST_ZIP);
          } else {
            recordPublishService.publishToHarvestQueue(
                harvestInputStream(content, datasetId, recordDataEncapsulated, path), Step.HARVEST_ZIP);
          }

          return ReportingIteration.IterationResult.CONTINUE;

        } catch (IOException | RuntimeException e) {
          exception.add(new ImmutablePair<>(path, e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
      });

      // Attempt to delete the temporary iterator content.
      iterator.deleteIteratorContent();

      datasetService.updateNumberOfTotalRecord(datasetId, numberOfIterations.get());

    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting records ", e);
    } finally {
      closeStream(inputStream);
    }

    if (!exception.isEmpty()) {
      throw new HarvesterException("Could not process path " + exception.get(0).getKey() + ".",
          exception.get(0).getValue());
    }
  }

  private RecordInfo harvestInputStream(InputStream inputStream, String datasetId, Record.RecordBuilder recordToHarvest, Path path)
      throws ServiceException {
    List<RecordError> recordErrors = new ArrayList<>();
    RecordEntity recordEntity = recordRepository.save(
        new RecordEntity(null, null, datasetId));

    try {
      Record harvestedRecord = recordToHarvest
          .providerId(path.toString())
          .content(new ByteArrayInputStream(IOUtils.toByteArray(inputStream)).readAllBytes())
          .recordId(recordEntity.getId())
          .build();

      return new RecordInfo(harvestedRecord, recordErrors);

    } catch (RuntimeException | IOException e) {
      LOGGER.error("Error harvesting File Record Header: {} with exception {}",
          recordEntity.getId(), e);
      recordErrors.add(new RecordError(
          "Error harvesting File Record Header:" + recordEntity.getId(),
          e.getMessage()));

      return new RecordInfo(recordToHarvest.build(), recordErrors);
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
