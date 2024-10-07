package eu.europeana.metis.sandbox.service.workflow;

import static java.util.stream.Collectors.groupingBy;

import eu.europeana.metis.debias.detect.client.DeBiasClient;
import eu.europeana.metis.debias.detect.model.error.ErrorDeBiasResult;
import eu.europeana.metis.debias.detect.model.request.DetectionParameter;
import eu.europeana.metis.debias.detect.model.response.DetectionDeBiasResult;
import eu.europeana.metis.debias.detect.model.response.ValueDetection;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.EuropeanaType;
import eu.europeana.metis.schema.jibx.EuropeanaType.Choice;
import eu.europeana.metis.schema.jibx.ProxyType;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.metis.schema.jibx.ResourceOrLiteralType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type DeBias process service.
 */
@Service
class DeBiasProcessServiceImpl implements DeBiasProcessService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeBiasProcessServiceImpl.class);

  private static final int DEBIAS_CLIENT_PARTITION_SIZE = 20;

  private final DeBiasClient deBiasClient;

  private final RecordDeBiasMainRepository recordDeBiasMainRepository;

  private final RecordDeBiasDetailRepository recordDeBiasDetailRepository;

  private final RecordRepository recordRepository;

  /**
   * Instantiates a new DeBias process service.
   *
   * @param deBiasClient the DeBias client
   * @param recordDeBiasMainRepository the record de bias main repository
   * @param recordDeBiasDetailRepository the record de bias detail repository
   * @param recordRepository the record repository
   */
  public DeBiasProcessServiceImpl(DeBiasClient deBiasClient,
      RecordDeBiasMainRepository recordDeBiasMainRepository,
      RecordDeBiasDetailRepository recordDeBiasDetailRepository,
      RecordRepository recordRepository) {
    this.deBiasClient = deBiasClient;
    this.recordDeBiasMainRepository = recordDeBiasMainRepository;
    this.recordDeBiasDetailRepository = recordDeBiasDetailRepository;
    this.recordRepository = recordRepository;
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
      deBiasReport.forEach(row -> {
            LOGGER.info("recordId: {} language: {} literal: {}", row.recordId(), row.valueDetection().getLanguage(), row.valueDetection().getLiteral());
            row.valueDetection().getTags().forEach(tag ->
                LOGGER.info("tag {} {} {} {}", tag.getStart(), tag.getEnd(), tag.getLength(), tag.getUri()));
          }
      );
      saveReport(deBiasReport);
    }
  }

  /**
   * Do DeBias and generate report.
   *
   * @param recordList the record list
   * @param deBiasReport the DeBias report
   */
  private void doDeBiasAndGenerateReport(List<Record> recordList, List<DeBiasReportRow> deBiasReport) {
    List<DeBiasInputRecord> values = getDescriptionsFromRecords(recordList);
    values.stream()
          .collect(groupingBy(DeBiasInputRecord::language))
          .forEach(((deBiasSupportedLanguage, recordDescriptions) ->
                // process by language in batches of DEBIAS_CLIENT_PARTITION_SIZE items per request
                partitionList(recordDescriptions, DEBIAS_CLIENT_PARTITION_SIZE)
                    .forEach(partition -> {
                          DetectionParameter detectionParameters = new DetectionParameter();
                          detectionParameters.setValues(partition.stream().map(DeBiasInputRecord::description).toList());
                          detectionParameters.setLanguage(deBiasSupportedLanguage.getCodeISO6391());
                          try {
                            switch (deBiasClient.detect(detectionParameters)) {
                              case DetectionDeBiasResult deBiasResult when deBiasResult.getDetections() != null -> {
                                for (int i = 0; i < partition.size(); i++) {
                                  deBiasReport.add(new DeBiasReportRow(partition.get(i).recordId(),
                                      deBiasResult.getDetections().get(i),
                                      partition.get(i).language(),
                                      partition.get(i).sourceField()));
                                }
                              }
                              case ErrorDeBiasResult errorDeBiasResult when errorDeBiasResult.getDetailList() != null ->
                                  errorDeBiasResult.getDetailList()
                                                   .forEach(detail ->
                                                       LOGGER.error("{} {} {}", detail.getMsg(), detail.getType(), detail.getLoc())
                                                   );
                              default -> LOGGER.info("DeBias detected nothing");
                            }
                          } catch (RuntimeException e) {
                            LOGGER.error(e.getMessage(), e);
                          }
                          LOGGER.info("DeBias execution finished for partition: {}",
                              partition.stream()
                                       .map(DeBiasInputRecord::recordId)
                                       .map(Object::toString)
                                       .collect(Collectors.joining(","))
                          );
                        }
                    )
              )
          );
  }

  /**
   * Save DeBias report into database.
   *
   * @param report the report
   */
  private void saveReport(List<DeBiasReportRow> report) {
    report.forEach( row -> {
      if (!row.valueDetection().getTags().isEmpty()) {
        RecordEntity recordEntity = recordRepository.findById(row.recordId()).orElse(null);
        RecordDeBiasMainEntity recordDeBiasMain = new RecordDeBiasMainEntity(recordEntity, row.valueDetection().getLiteral(),
            Language.valueOf(row.valueDetection().getLanguage().toUpperCase(Locale.US)), row.sourceField());
        recordDeBiasMainRepository.save(recordDeBiasMain);
        row.valueDetection()
           .getTags()
           .forEach(tag -> {
             RecordDeBiasDetailEntity recordDeBiasDetail = new RecordDeBiasDetailEntity(recordDeBiasMain,
                 tag.getStart(), tag.getEnd(), tag.getLength(), tag.getUri());
             recordDeBiasDetailRepository.save(recordDeBiasDetail);
           });
      }
    });
  }

  /**
   * Gets descriptions from records.
   *
   * @param recordList the record list
   * @return the descriptions from records
   */
  private List<DeBiasInputRecord> getDescriptionsFromRecords(List<Record> recordList) {
    return recordList
        .stream()
        .map(recordToProcess -> {
          List<DeBiasInputRecord> deBiasInputRecords;
          try {
            deBiasInputRecords = getDescriptionsAndLanguageFromRdf(
                new RdfConversionUtils()
                    .convertStringToRdf(
                        new String(recordToProcess.getContent(), StandardCharsets.UTF_8)
                    ), recordToProcess.getRecordId()
            );
          } catch (SerializationException e) {
            deBiasInputRecords = Collections.emptyList();
          }
          return deBiasInputRecords;
        })
        .flatMap(Collection::stream)
        .toList();
  }

  /**
   * Gets descriptions and language from rdf.
   *
   * @param rdf the rdf
   * @param recordId the record id
   * @return the descriptions and language from rdf
   */
  private List<DeBiasInputRecord> getDescriptionsAndLanguageFromRdf(RDF rdf, Long recordId) {
    List<ProxyType> providerProxies = this.getProviderProxies(rdf);
    List<EuropeanaType.Choice> choices = providerProxies
        .stream()
        .map(EuropeanaType::getChoiceList)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .toList();

    return this.getChoicesInStringList(choices,
        EuropeanaType.Choice::ifDescription,
        EuropeanaType.Choice::getDescription,
        ResourceOrLiteralType::getString,
        value -> value.getLang().getLang(),
        DeBiasSourceField.DC_DESCRIPTION,
        recordId);
  }

  /**
   * Is provider proxy boolean.
   *
   * @param proxy the proxy
   * @return the boolean
   */
  private boolean isProviderProxy(ProxyType proxy) {
    return proxy.getEuropeanaProxy() == null || BooleanUtils.isFalse(proxy.getEuropeanaProxy().isEuropeanaProxy());
  }

  /**
   * Gets provider proxies.
   *
   * @param rdf the rdf
   * @return the provider proxies
   */
  private List<ProxyType> getProviderProxies(RDF rdf) {
    return Optional.ofNullable(rdf.getProxyList()).stream()
                   .flatMap(Collection::stream)
                   .filter(Objects::nonNull)
                   .filter(this::isProviderProxy)
                   .toList();
  }

  /**
   * Gets choices in string list.
   *
   * @param <T> the type parameter
   * @param choices the choices
   * @param choicePredicate the choice predicate
   * @param choiceGetter the choice getter
   * @param getString the get string
   * @param getLanguage the get language
   * @param recordId the record id
   * @return the choices in string list
   */
  private <T> List<DeBiasInputRecord> getChoicesInStringList(List<EuropeanaType.Choice> choices,
      Predicate<Choice> choicePredicate, Function<Choice, T> choiceGetter,
      Function<T, String> getString, Function<T, String> getLanguage, DeBiasSourceField sourceField, Long recordId) {
    return choices.stream()
                  .filter(Objects::nonNull)
                  .filter(choicePredicate)
                  .map(choiceGetter)
                  .map(value -> Optional
                      .ofNullable(DeBiasSupportedLanguage.match(getLanguage.apply(value)))
                      .map(lang -> new DeBiasInputRecord(recordId, getString.apply(value), lang, sourceField))
                      .orElse(null))
                  .filter(Objects::nonNull)
                  .toList();
  }

  /**
   * The type DeBias input record.
   */
  record DeBiasInputRecord(Long recordId, String description, DeBiasSupportedLanguage language, DeBiasSourceField sourceField) {

  }

  /**
   * The type DeBias report row.
   */
  record DeBiasReportRow(Long recordId, ValueDetection valueDetection, DeBiasSupportedLanguage language, DeBiasSourceField sourceField) {

  }

  /**
   * Partition list stream.
   *
   * @param <T> the type parameter
   * @param sourceList the source
   * @param partitionSize the batch size
   * @return the stream
   */
  static <T> Stream<List<T>> partitionList(List<T> sourceList, int partitionSize) {
    if (partitionSize <= 0) {
      throw new IllegalArgumentException(
          String.format("The partition size cannot be smaller than 0. Actual value: %s", partitionSize));
    }
    if (sourceList.isEmpty()) {
      return Stream.empty();
    }
    int partitions = (sourceList.size() - 1) / partitionSize;
    return IntStream.rangeClosed(0, partitions)
                    .mapToObj(partition -> {
                      int startIndex = partition * partitionSize;
                      int endIndex = (partition == partitions) ? sourceList.size() : (partition + 1) * partitionSize;
                      return sourceList.subList(startIndex, endIndex);
                    });
  }
}
