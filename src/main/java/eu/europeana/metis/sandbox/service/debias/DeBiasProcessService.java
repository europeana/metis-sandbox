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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type DeBias process service.
 */
@Service
public class DeBiasProcessService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int DEBIAS_CLIENT_PARTITION_SIZE = 20;

  private final DeBiasClient deBiasClient;

  private final RecordDeBiasMainRepository recordDeBiasMainRepository;

  private final RecordDeBiasDetailRepository recordDeBiasDetailRepository;
  private final DatasetRepository datasetRepository;

  /**
   * Instantiates a new DeBias process service.
   *
   * @param deBiasClient the DeBias client
   * @param recordDeBiasMainRepository the record de bias main repository
   * @param recordDeBiasDetailRepository the record de bias detail repository
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

  @Transactional
  public void process(String recordContent, String datasetId, String recordId) {

    List<DeBiasInputRecord> deBiasInputRecords = getDeBiasSourceFieldsFromRecords(
        recordContent, recordId);
    List<DeBiasReportRow> deBiasReportRows = doDeBiasAndGenerateReport(deBiasInputRecords);

    if (!deBiasReportRows.isEmpty()) {
      logReport(deBiasReportRows);
      saveReport(deBiasReportRows, datasetId, recordId);
    }
  }

  public List<DeBiasReportRow> doDeBiasAndGenerateReport(List<DeBiasInputRecord> deBiasInputRecords) {
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
                          detail -> LOGGER.error("{} {} {}", detail.getMsg(), detail.getType(), detail.getLoc()));
                  default -> LOGGER.info("DeBias detected nothing");
                }
              } catch (RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
              }
              LOGGER.info("DeBias execution finished for partition: {}",
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
      LOGGER.info("europeanaId: {} language: {} source: {} literal: {}",
          row.europeanaId(), row.valueDetection().getLanguage(),
          row.sourceField(), row.valueDetection().getLiteral());
      row.valueDetection().getTags()
         .forEach(tag -> LOGGER.info("tag {} {} {} {}",
             tag.getStart(), tag.getEnd(), tag.getLength(), tag.getUri()));
    });
  }


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
        LOGGER.error("Serialization {}", e.getMessage(), e);
      }
      return deBiasInputRecords;
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
