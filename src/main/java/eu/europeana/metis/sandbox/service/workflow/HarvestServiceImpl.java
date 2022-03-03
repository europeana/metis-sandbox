package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.oaipmh.*;
import eu.europeana.metis.sandbox.common.HarvestContent;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import eu.europeana.metis.sandbox.service.dataset.AsyncRecordPublishService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestServiceImpl implements HarvestService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HarvestServiceImpl.class);

  private final HttpHarvester httpHarvester;
  private final OaiHarvester oaiHarvester;
  private final AsyncRecordPublishService asyncRecordPublishService;
  private final DatasetService datasetService;
  private final int maxRecords;

  private final RecordRepository recordRepository;


  @Autowired
  public HarvestServiceImpl(HttpHarvester httpHarvester, OaiHarvester oaiHarvester,
                            AsyncRecordPublishService asyncRecordPublishService,
                            DatasetService datasetService, @Value("${sandbox.dataset.max-size}") int maxRecords,
                            RecordRepository recordRepository) {
    this.httpHarvester = httpHarvester;
    this.asyncRecordPublishService = asyncRecordPublishService;
    this.datasetService = datasetService;
    this.recordRepository = recordRepository;
    this.httpHarvester.setMaxNumberOfIterations(maxRecords);
    this.oaiHarvester = oaiHarvester;
    this.maxRecords = maxRecords;

  }

  @Override
  public HarvestContent harvestZipMultipartFile(MultipartFile file) throws ServiceException {

    HarvestContent pairResult;

    try {
      pairResult = this.harvest(file.getInputStream());
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from file " + file.getName(), e);
    }
    return pairResult;
  }

  @Override
  public HarvestContent harvestZipUrl(String url) throws ServiceException {

    HarvestContent harvestContent;

    try (InputStream input = new URL(url).openStream()) {
      harvestContent = this.harvest(input);
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from " + url, e);
    }
    return harvestContent;
  }

  @Override
  public void harvestOaiPmh(String datasetName, String datasetId,
                             Country country, Language language, OaiHarvestData oaiHarvestData) {
    try (OaiRecordHeaderIterator recordHeaderIterator = oaiHarvester
            .harvestRecordHeaders(
                    new OaiHarvest(oaiHarvestData.getUrl(), oaiHarvestData.getMetadataformat(), oaiHarvestData.getSetspec()))) {

      AtomicInteger currentNumberOfIterations = new AtomicInteger();

      Record.RecordBuilder recordDataEncapsulated = Record.builder()
              .country(country)
              .language(language)
              .datasetName(datasetName)
              .datasetId(datasetId);

      recordHeaderIterator.forEach(recordHeader -> {
        OaiHarvestData completeOaiHarvestData = new OaiHarvestData(oaiHarvestData.getUrl(),
                oaiHarvestData.getSetspec(),
                oaiHarvestData.getMetadataformat(),
                recordHeader.getOaiIdentifier());
        currentNumberOfIterations.getAndIncrement();

        if (currentNumberOfIterations.get() > maxRecords) {
          datasetService.updateRecordsLimitExceededToTrue(datasetId);
          currentNumberOfIterations.set(maxRecords);
          return ReportingIteration.IterationResult.TERMINATE;
        }

        if (datasetService.isXsltPresent(datasetId)) {
          asyncRecordPublishService.publishWithXslt(harvestOaiRecordHeader(datasetId, completeOaiHarvestData,
                  recordDataEncapsulated), Step.HARVEST_OAI_PMH);
        } else {
          asyncRecordPublishService.publishWithoutXslt(harvestOaiRecordHeader(datasetId, completeOaiHarvestData,
                  recordDataEncapsulated), Step.HARVEST_OAI_PMH);

        }

        return ReportingIteration.IterationResult.CONTINUE;
      });

      datasetService.updateNumberOfTotalRecord(datasetId, currentNumberOfIterations.get());

    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records ", e);
    }
  }


  @Override
  public RecordInfo harvestOaiRecordHeader(String datasetId, OaiHarvestData oaiHarvestData, Record.RecordBuilder recordToHarvest) {

    List<RecordError> recordErrors = new ArrayList<>();
    try {
      OaiRepository oaiRepository = new OaiRepository(oaiHarvestData.getUrl(),
          oaiHarvestData.getMetadataformat());
      OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiRepository,
          oaiHarvestData.getOaiIdentifier());
      RecordEntity recordEntity = recordRepository.save(
          new RecordEntity(null, null, datasetId));
      Record harvestedRecord = recordToHarvest
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

  private HarvestContent harvest(InputStream inputStream) throws ServiceException {

    List<ByteArrayInputStream> records = new ArrayList<>();
    AtomicBoolean hasReachedRecordLimit = new AtomicBoolean(false);
    AtomicInteger numberOfIterations = new AtomicInteger(0);

    try {
      httpHarvester.harvestRecords(inputStream, CompressedFileExtension.ZIP, entry -> {
        numberOfIterations.getAndIncrement();
        if (numberOfIterations.get() > maxRecords) {
          hasReachedRecordLimit.set(true);
        } else {
          final byte[] content = entry.getEntryContent().readAllBytes();
          records.add(new ByteArrayInputStream(content));
        }
      });

    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting records ", e);
    }

    if (records.isEmpty()) {
      throw new ServiceException("Provided file does not contain any records", null);
    }
    return new HarvestContent(hasReachedRecordLimit, records);
  }

}
