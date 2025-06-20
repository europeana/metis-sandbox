package eu.europeana.metis.sandbox.service.debias;

import static eu.europeana.metis.sandbox.service.debias.DeBiasRdfInfoExtractor.partitionList;
import static java.util.stream.Collectors.groupingBy;

import eu.europeana.metis.debias.detect.client.DeBiasClient;
import eu.europeana.metis.debias.detect.model.error.ErrorDeBiasResult;
import eu.europeana.metis.debias.detect.model.request.BiasInputLiterals;
import eu.europeana.metis.debias.detect.model.response.DetectionDeBiasResult;
import eu.europeana.metis.debias.detect.model.response.ValueDetection;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.RDF;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class responsible for performing debiasing operations on records.
 */
@Slf4j
@Service
public class DeBiasProcessService {

  private static final int DEBIAS_CLIENT_PARTITION_SIZE = 20;

  private final DeBiasClient deBiasClient;

  private final RecordDeBiasMainRepository recordDeBiasMainRepository;

  private final RecordDeBiasDetailRepository recordDeBiasDetailRepository;
  private final DatasetRepository datasetRepository;

  /**
   * Constructor.
   *
   * @param deBiasClient the client responsible for de-bias related operations
   * @param recordDeBiasMainRepository repository for managing main de-bias records
   * @param recordDeBiasDetailRepository repository for managing detailed de-bias records
   * @param datasetRepository repository for managing datasets
   */
  public DeBiasProcessService(DeBiasClient deBiasClient,
      RecordDeBiasMainRepository recordDeBiasMainRepository,
      RecordDeBiasDetailRepository recordDeBiasDetailRepository,
      DatasetRepository datasetRepository) {
    this.deBiasClient = deBiasClient;
    this.recordDeBiasMainRepository = recordDeBiasMainRepository;
    this.recordDeBiasDetailRepository = recordDeBiasDetailRepository;
    this.datasetRepository = datasetRepository;
  }

  /**
   * Performs debiasing of a record, generates a report, and persists results.
   *
   * @param recordContent the content of the record to process
   * @param datasetId the identifier of the dataset the record belongs to
   * @param recordId the identifier of the record to process
   */
  @Transactional
  public void process(String recordContent, String datasetId, String recordId) {

    List<DeBiasInputRecord> deBiasInputRecords = getDeBiasSourceFieldsFromRecords(
        recordContent, recordId);
    List<DeBiasReportRow> deBiasReportRows = performDeBiasAndGenerateReport(deBiasInputRecords);

    if (!deBiasReportRows.isEmpty()) {
      logReport(deBiasReportRows);
      saveReport(deBiasReportRows, datasetId, recordId);
    }
  }

  /**
   * Extracts debiasing source fields from the provided record content in RDF format.
   *
   * @param recordContent the RDF content of the record to be processed
   * @param recordId the unique identifier of the record
   * @return a list of {@code DeBiasInputRecord} containing extracted field data
   */
  public List<DeBiasInputRecord> getDeBiasSourceFieldsFromRecords(String recordContent, String recordId) {
    List<DeBiasInputRecord> deBiasInputRecords = new ArrayList<>();
    try {
      RDF rdf = new RdfConversionUtils().convertStringToRdf(recordContent);
      DeBiasRdfInfoExtractor deBiasRdfInfoExtractor = new DeBiasRdfInfoExtractor(rdf, recordId);

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
      log.error("Serialization {}", e.getMessage(), e);
    }
    return deBiasInputRecords;
  }

  /**
   * Processes a list of de-bias input records, executes debiasing tasks in batches by language, and generates a report containing
   * detection results.
   *
   * <p>This method partitions input records by supported language, processes each partition
   * for debias detection, and creates report rows with de-bias results.
   *
   * @param deBiasInputRecords list of input records containing details for de-biasing
   * @return a list of report rows with the results of the de-bias detection process
   */
  public List<DeBiasReportRow> performDeBiasAndGenerateReport(List<DeBiasInputRecord> deBiasInputRecords) {
    List<DeBiasReportRow> deBiasReportRows = new ArrayList<>();

    deBiasInputRecords
        .stream()
        .collect(groupingBy(DeBiasInputRecord::language))
        .forEach(((deBiasSupportedLanguage, recordDescriptions) ->
            // process by language in batches of DEBIAS_CLIENT_PARTITION_SIZE items per request
            partitionList(recordDescriptions, DEBIAS_CLIENT_PARTITION_SIZE).forEach(partition -> {
              BiasInputLiterals biasInputLiterals = new BiasInputLiterals();
              biasInputLiterals.setUseLLM(true);
              biasInputLiterals.setUseNER(true);
              biasInputLiterals.setValues(partition.stream().map(DeBiasInputRecord::literal).toList());
              biasInputLiterals.setLanguage(deBiasSupportedLanguage.getCodeISO6391());
              try {
                switch (deBiasClient.detect(biasInputLiterals)) {
                  case DetectionDeBiasResult deBiasResult when deBiasResult.getDetections() != null -> {
                    for (int i = 0; i < partition.size(); i++) {
                      deBiasReportRows.add(
                          new DeBiasReportRow(partition.get(i).europeanaId(),
                              deBiasResult.getDetections().get(i), partition.get(i).sourceField())
                      );
                    }
                  }
                  case ErrorDeBiasResult errorDeBiasResult when errorDeBiasResult.getDetailList() != null ->
                      errorDeBiasResult.getDetailList().forEach(
                          detail -> log.error("{} {} {}", detail.getMsg(), detail.getType(), detail.getLoc()));
                  default -> log.info("DeBias detected nothing");
                }
              } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
              }
              log.info("DeBias execution finished for partition: {}",
                  partition.stream()
                           .map(DeBiasInputRecord::europeanaId)
                           .map(Object::toString)
                           .collect(Collectors.joining(",")));
            })));
    return deBiasReportRows;
  }

  private void saveReport(List<DeBiasReportRow> report, String datasetId, String recordId) {
    DatasetEntity dataset = datasetRepository.findById(Integer.valueOf(datasetId)).orElseThrow();
    report.forEach(row -> {
      if (!row.valueDetection().getTags().isEmpty()) {
        RecordDeBiasMainEntity recordDeBiasMain = new RecordDeBiasMainEntity(dataset, recordId, row.valueDetection().getLiteral(),
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
      log.info("europeanaId: {} language: {} source: {} literal: {}",
          row.europeanaId(), row.valueDetection().getLanguage(),
          row.sourceField(), row.valueDetection().getLiteral());
      row.valueDetection().getTags()
         .forEach(tag -> log.info("tag {} {} {} {}",
             tag.getStart(), tag.getEnd(), tag.getLength(), tag.getUri()));
    });
  }


  /**
   * The type DeBias input record.
   */
  public record DeBiasInputRecord(String europeanaId, String literal,
                                  DeBiasSupportedLanguage language,
                                  DeBiasSourceField sourceField) {

  }

  /**
   * The type DeBias report row.
   */
  public record DeBiasReportRow(String europeanaId,
                                ValueDetection valueDetection,
                                DeBiasSourceField sourceField) {

  }
}
