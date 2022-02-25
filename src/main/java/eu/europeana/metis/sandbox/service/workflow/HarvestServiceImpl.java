package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.HarvestContent;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestServiceImpl implements HarvestService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final HttpHarvester httpHarvester;

  private final OaiHarvester oaiHarvester;

  private final int maxRecords;

  private final RecordRepository recordRepository;


  @Autowired
  public HarvestServiceImpl(HttpHarvester httpHarvester, OaiHarvester oaiHarvester,
                            @Value("${sandbox.dataset.max-size}") int maxRecords, RecordRepository recordRepository) {
    this.httpHarvester = httpHarvester;
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
  public RecordInfo harvestOaiRecordHeader(OaiHarvestData oaiHarvestData, Record recordToHarvest,
      OaiRecordHeader oaiRecordHeader) {

    List<RecordError> recordErrors = new ArrayList<>();
    try {
      OaiRepository oaiRepository = new OaiRepository(oaiHarvestData.getUrl(), oaiHarvestData.getMetadataformat());
      OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiRepository, oaiRecordHeader.getOaiIdentifier());
      RecordEntity recordEntity = recordRepository.save(new RecordEntity(null, null, recordToHarvest.getDatasetId()));
      Record harvestedRecord = Record.builder()
              .content(oaiRecord.getRecord().readAllBytes())
              .recordId(recordEntity.getId())
              .country(recordToHarvest.getCountry())
              .datasetId(recordToHarvest.getDatasetId())
              .language(recordToHarvest.getLanguage())
              .datasetName(recordToHarvest.getDatasetName())
              .build();
      return new RecordInfo(harvestedRecord, recordErrors);

    } catch (HarvesterException | IOException e) {
      logger.error("Error harvesting OAI-PMH Record Header: {} with exception {}",
          oaiRecordHeader.getOaiIdentifier(), e);
      recordErrors.add(new RecordError(
          "Error harvesting OAI-PMH Record Header:" + oaiRecordHeader.getOaiIdentifier(),
          e.getMessage()));

      return new RecordInfo(recordToHarvest, recordErrors);
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
