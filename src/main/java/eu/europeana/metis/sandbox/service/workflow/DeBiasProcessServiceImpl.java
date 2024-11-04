package eu.europeana.metis.sandbox.service.workflow;

import static eu.europeana.metis.sandbox.service.workflow.DeBiasRdfInfoExtractor.partitionList;
import static java.util.stream.Collectors.groupingBy;

import eu.europeana.metis.debias.detect.client.DeBiasClient;
import eu.europeana.metis.debias.detect.model.error.ErrorDeBiasResult;
import eu.europeana.metis.debias.detect.model.request.BiasInputLiterals;
import eu.europeana.metis.debias.detect.model.response.DetectionDeBiasResult;
import eu.europeana.metis.debias.detect.model.response.ValueDetection;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.RDF;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type DeBias process service.
 */
@Service
public class DeBiasProcessServiceImpl implements DeBiasProcessService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeBiasProcessServiceImpl.class);

  private static final int DEBIAS_CLIENT_PARTITION_SIZE = 20;

  private final DeBiasClient deBiasClient;

  private final RecordDeBiasMainRepository recordDeBiasMainRepository;

  private final RecordDeBiasDetailRepository recordDeBiasDetailRepository;

  private final RecordRepository recordRepository;

  private final RecordLogRepository recordLogRepository;

  private final DatasetDeBiasRepository datasetDeBiasRepository;

  private final Map<String, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();

  private final LockRegistry lockRegistry;

  /**
   * Instantiates a new DeBias process service.
   *
   * @param deBiasClient the DeBias client
   * @param recordDeBiasMainRepository the record de bias main repository
   * @param recordDeBiasDetailRepository the record de bias detail repository
   * @param datasetDeBiasRepository the dataset de bias repository
   * @param recordLogRepository the record log repository
   * @param recordRepository the record repository
   * @param lockRegistry the lock registry
   */
  public DeBiasProcessServiceImpl(DeBiasClient deBiasClient,
      RecordDeBiasMainRepository recordDeBiasMainRepository,
      RecordDeBiasDetailRepository recordDeBiasDetailRepository,
      DatasetDeBiasRepository datasetDeBiasRepository,
      RecordLogRepository recordLogRepository,
      RecordRepository recordRepository,
      LockRegistry lockRegistry) {
    this.deBiasClient = deBiasClient;
    this.recordDeBiasMainRepository = recordDeBiasMainRepository;
    this.recordDeBiasDetailRepository = recordDeBiasDetailRepository;
    this.recordRepository = recordRepository;
    this.recordLogRepository = recordLogRepository;
    this.datasetDeBiasRepository = datasetDeBiasRepository;
    this.lockRegistry = lockRegistry;
  }

  /**
   * Process batch of records with DeBias Tool and generate report
   *
   * @param recordList the records to process
   */
  @Transactional
  @Override
  public void process(List<Record> recordList) {
    Objects.requireNonNull(recordList, "List of records is required");
    List<DeBiasReportRow> deBiasReport = new ArrayList<>();

    doDeBiasAndGenerateReport(recordList, deBiasReport);

    if (!deBiasReport.isEmpty()) {
      logReport(deBiasReport);
      saveReport(deBiasReport);
    }
    updateProgress(recordList);
  }

  /**
   * Update progress
   */
  private void updateProgress(List<Record> recordList) {
    recordList.stream()
              .collect(groupingBy(Record::getDatasetId))
              .forEach((datasetId, records) -> {
                    LOGGER.info("========================Updating DeBias progress for datasetId: {}========================",
                        datasetId);
                    records.forEach(recordToProcess ->
                        recordLogRepository.updateByRecordIdAndStepAndStatus(recordToProcess.getRecordId(), Step.DEBIAS,
                            Status.SUCCESS));
                    updateDeBiasProgressCounters(datasetId);
                  }
              );
  }

  private void updateDeBiasProgressCounters(String datasetId) {
    final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> lockRegistry.obtain("debiasUpdateCounters_" + datasetId));
    try {
      lock.lock();
      LOGGER.info("DeBias counters: {} lock, Locked", datasetId);
      final int totalDeBias = recordLogRepository.getTotalDeBiasCounterByDatasetId(datasetId);
      final int progressDeBias = recordLogRepository.getProgressDeBiasCounterByDatasetId(datasetId);
      LOGGER.info("DeBias PROGRESS datasetId: {}/{}", progressDeBias, totalDeBias);
      if (progressDeBias == totalDeBias) {
        datasetDeBiasRepository.updateState(Integer.parseInt(datasetId), "COMPLETED");
        LOGGER.info("DeBias COMPLETED datasetId: {}", datasetId);
      } else {
        datasetDeBiasRepository.updateState(Integer.parseInt(datasetId), "PROCESSING");
      }
    } finally {
      lock.unlock();
      LOGGER.info("DeBias counters: {} lock, Unlocked", datasetId);
    }
  }

  /**
   * Do DeBias and generate report.
   *
   * @param recordList the record list
   * @param deBiasReport the DeBias report
   */
  private void doDeBiasAndGenerateReport(List<Record> recordList, List<DeBiasReportRow> deBiasReport) {
    List<DeBiasInputRecord> deBiasSourceFieldsFromRecords = getDeBiasSourceFieldsFromRecords(recordList);
    deBiasSourceFieldsFromRecords
        .stream()
        .collect(groupingBy(DeBiasInputRecord::language))
        .forEach(((deBiasSupportedLanguage, recordDescriptions) ->
            // process by language in batches of DEBIAS_CLIENT_PARTITION_SIZE items per request
            partitionList(recordDescriptions, DEBIAS_CLIENT_PARTITION_SIZE).forEach(partition -> {
              BiasInputLiterals biasInputLiterals = new BiasInputLiterals();
              biasInputLiterals.setValues(partition.stream().map(DeBiasInputRecord::literal).toList());
              biasInputLiterals.setLanguage(deBiasSupportedLanguage.getCodeISO6391());
              try {
                switch (deBiasClient.detect(biasInputLiterals)) {
                  case DetectionDeBiasResult deBiasResult when deBiasResult.getDetections() != null -> {
                    for (int i = 0; i < partition.size(); i++) {
                      deBiasReport.add(
                          new DeBiasReportRow(partition.get(i).recordId(), partition.get(i).europeanaId(),
                              deBiasResult.getDetections().get(i), partition.get(i).sourceField())
                      );
                    }
                  }
                  case ErrorDeBiasResult errorDeBiasResult when errorDeBiasResult.getDetailList() != null ->
                      errorDeBiasResult.getDetailList().forEach(
                          detail -> LOGGER.error("{} {} {}", detail.getMsg(), detail.getType(), detail.getLoc()));
                  default -> LOGGER.info("DeBias detected nothing");
                }
              } catch (RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
              }
              LOGGER.info("DeBias execution finished for partition: {}",
                  partition.stream()
                           .map(DeBiasInputRecord::recordId)
                           .map(Object::toString)
                           .collect(Collectors.joining(",")));
            })));
  }

  /**
   * Save DeBias report into database.
   *
   * @param report the report
   */
  private void saveReport(List<DeBiasReportRow> report) {
    report.forEach(row -> {
      if (!row.valueDetection().getTags().isEmpty()) {
        RecordEntity recordEntity = recordRepository.findById(row.recordId()).orElse(null);
        RecordDeBiasMainEntity recordDeBiasMain = new RecordDeBiasMainEntity(recordEntity, row.valueDetection().getLiteral(),
            Language.valueOf(row.valueDetection().getLanguage().toUpperCase(Locale.US)), row.sourceField());
        recordDeBiasMainRepository.save(recordDeBiasMain);
        row.valueDetection().getTags().forEach(tag -> {
          RecordDeBiasDetailEntity recordDeBiasDetail = new RecordDeBiasDetailEntity(recordDeBiasMain, tag.getStart(),
              tag.getEnd(), tag.getLength(), tag.getUri());
          recordDeBiasDetailRepository.save(recordDeBiasDetail);
        });
      }
    });
  }

  /**
   * Log report.
   *
   * @param deBiasReport the de bias report
   */
  private void logReport(List<DeBiasReportRow> deBiasReport) {
    deBiasReport.forEach(row -> {
      LOGGER.info("recordId: {} europeanaId: {} language: {} source: {} literal: {}",
          row.recordId(), row.europeanaId(), row.valueDetection().getLanguage(),
          row.sourceField(), row.valueDetection().getLiteral());
      row.valueDetection().getTags()
         .forEach(tag -> LOGGER.info("tag {} {} {} {}",
             tag.getStart(), tag.getEnd(), tag.getLength(), tag.getUri()));
    });
  }

  /**
   * Gets descriptions from records.
   *
   * @param recordList the record list
   * @return the descriptions from records
   */
  private List<DeBiasInputRecord> getDeBiasSourceFieldsFromRecords(List<Record> recordList) {
    return recordList.stream().map(recordToProcess -> {
      List<DeBiasInputRecord> deBiasInputRecords = new ArrayList<>();
      try {
        RDF rdf = new RdfConversionUtils().convertStringToRdf(new String(recordToProcess.getContent(), StandardCharsets.UTF_8));
        DeBiasRdfInfoExtractor deBiasRdfInfoExtractor = new DeBiasRdfInfoExtractor(rdf, recordToProcess);

        // Get the literal values
        deBiasInputRecords.addAll(deBiasRdfInfoExtractor.getDescriptionsAndLanguageFromRdf());
        deBiasInputRecords.addAll(deBiasRdfInfoExtractor.getTitlesAndLanguageFromRdf());
        deBiasInputRecords.addAll(deBiasRdfInfoExtractor.getAlternativeAndLanguageFromRdf());
        deBiasInputRecords.addAll(deBiasRdfInfoExtractor.getSubjectAndLanguageFromRdf());
        deBiasInputRecords.addAll(deBiasRdfInfoExtractor.getTypeAndLanguageFromRdf());

        // Get the literal values that are linked through contextual classes.
        deBiasInputRecords.addAll(deBiasRdfInfoExtractor.getSubjectReferencesAndTypeReferencesFromRdf());

      } catch (SerializationException e) {
        deBiasInputRecords = Collections.emptyList();
        LOGGER.error("Serialization {}", e.getMessage(), e);
      }
      return deBiasInputRecords;
    }).flatMap(Collection::stream).toList();
  }

  /**
   * The type DeBias input record.
   */
  record DeBiasInputRecord(Long recordId, String europeanaId, String literal,
                           DeBiasSupportedLanguage language,
                           DeBiasSourceField sourceField) {

  }

  /**
   * The type DeBias report row.
   */
  public record DeBiasReportRow(Long recordId, String europeanaId,
                                ValueDetection valueDetection,
                                DeBiasSourceField sourceField) {

  }
}
