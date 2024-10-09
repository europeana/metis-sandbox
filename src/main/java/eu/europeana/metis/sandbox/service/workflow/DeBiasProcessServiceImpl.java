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
import eu.europeana.metis.schema.jibx.AboutType;
import eu.europeana.metis.schema.jibx.Concept;
import eu.europeana.metis.schema.jibx.EuropeanaType;
import eu.europeana.metis.schema.jibx.EuropeanaType.Choice;
import eu.europeana.metis.schema.jibx.LiteralType;
import eu.europeana.metis.schema.jibx.LiteralType.Lang;
import eu.europeana.metis.schema.jibx.PrefLabel;
import eu.europeana.metis.schema.jibx.ProxyType;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.metis.schema.jibx.ResourceOrLiteralType;
import eu.europeana.metis.schema.jibx.ResourceOrLiteralType.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            LOGGER.info("recordId: {} europeanaId: {} language: {} source: {} literal: {}",
                row.recordId(), row.europeanaId(), row.valueDetection().getLanguage(),
                row.sourceField(), row.valueDetection().getLiteral());
            row.valueDetection().getTags().forEach(tag ->
                LOGGER.info("tag {} {} {} {}",
                    tag.getStart(), tag.getEnd(), tag.getLength(), tag.getUri()));
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
    List<DeBiasInputRecord> values = getDeBiasSourceFieldsFromRecords(recordList);
    values.stream()
          .collect(groupingBy(DeBiasInputRecord::language))
          .forEach(((deBiasSupportedLanguage, recordDescriptions) ->
                  // process by language in batches of DEBIAS_CLIENT_PARTITION_SIZE items per request
                  partitionList(recordDescriptions, DEBIAS_CLIENT_PARTITION_SIZE)
                      .forEach(partition -> {
                            DetectionParameter detectionParameters = new DetectionParameter();
                            detectionParameters.setValues(partition.stream().map(DeBiasInputRecord::literal).toList());
                            detectionParameters.setLanguage(deBiasSupportedLanguage.getCodeISO6391());
                            try {
                              switch (deBiasClient.detect(detectionParameters)) {
                                case DetectionDeBiasResult deBiasResult when deBiasResult.getDetections() != null -> {
                                  for (int i = 0; i < partition.size(); i++) {
                                    deBiasReport.add(new DeBiasReportRow(partition.get(i).recordId(),
                                        partition.get(i).europeanaId(),
                                        deBiasResult.getDetections().get(i),
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
    report.forEach(row -> {
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
  private List<DeBiasInputRecord> getDeBiasSourceFieldsFromRecords(List<Record> recordList) {
    return recordList
        .stream()
        .map(recordToProcess -> {
          List<DeBiasInputRecord> deBiasInputRecords = new ArrayList<>();
          try {
            RDF rdf = new RdfConversionUtils()
                .convertStringToRdf(
                    new String(recordToProcess.getContent(), StandardCharsets.UTF_8)
                );

            // Get the literal values
            deBiasInputRecords.addAll(getDescriptionsAndLanguageFromRdf(rdf, recordToProcess.getRecordId(), recordToProcess.getEuropeanaId()));
            deBiasInputRecords.addAll(getTitlesAndLanguageFromRdf(rdf, recordToProcess.getRecordId(), recordToProcess.getEuropeanaId()));
            deBiasInputRecords.addAll(getAlternativeAndLanguageFromRdf(rdf, recordToProcess.getRecordId(), recordToProcess.getEuropeanaId()));
            deBiasInputRecords.addAll(getSubjectAndLanguageFromRdf(rdf, recordToProcess.getRecordId(), recordToProcess.getEuropeanaId()));
            deBiasInputRecords.addAll(getTypeAndLanguageFromRdf(rdf, recordToProcess.getRecordId(), recordToProcess.getEuropeanaId()));

            // Get the values that are linked through contextual classes.
            Map<String, List<PrefLabel>> contextualClassesLabels = getContextualClassLabelsByRdfAbout(rdf);
            deBiasInputRecords.addAll(
                getChoices(rdf).stream().filter(Choice::ifSubject).map(Choice::getSubject)
                    .filter(Objects::nonNull).map(ResourceOrLiteralType::getResource)
                    .filter(Objects::nonNull).map(Resource::getResource)
                    .filter(Objects::nonNull).map(contextualClassesLabels::get)
                    .filter(Objects::nonNull).flatMap(Collection::stream).map(prefLabel ->
                       Optional.ofNullable(prefLabel.getLang()).map(Lang::getLang).map(DeBiasSupportedLanguage::match)
                            .map(lang -> new DeBiasInputRecord(recordToProcess.getRecordId(), recordToProcess.getEuropeanaId(), prefLabel.getString(), lang, DeBiasSourceField.DC_SUBJECT))
                            .orElse(null))
                .filter(Objects::nonNull).toList());
            deBiasInputRecords.addAll(
                getChoices(rdf).stream().filter(Choice::ifType).map(Choice::getType)
                    .filter(Objects::nonNull).map(ResourceOrLiteralType::getResource)
                    .filter(Objects::nonNull).map(Resource::getResource)
                    .filter(Objects::nonNull).map(contextualClassesLabels::get)
                    .filter(Objects::nonNull).flatMap(Collection::stream).map(prefLabel ->
                        Optional.ofNullable(prefLabel.getLang()).map(Lang::getLang).map(DeBiasSupportedLanguage::match)
                            .map(lang -> new DeBiasInputRecord(recordToProcess.getRecordId(), recordToProcess.getEuropeanaId(), prefLabel.getString(), lang, DeBiasSourceField.DC_TYPE))
                            .orElse(null))
                    .filter(Objects::nonNull).toList());
          } catch (SerializationException e) {
            deBiasInputRecords = Collections.emptyList();
          }
          return deBiasInputRecords;
        })
        .flatMap(Collection::stream)
        .toList();
  }

  private Map<String, List<PrefLabel>> getContextualClassLabelsByRdfAbout(RDF record){
    final Map<String, List<PrefLabel>> result = new HashMap<>();
    Optional.ofNullable(record.getAgentList()).stream().flatMap(Collection::stream)
        .forEach(agent -> result.put(agent.getAbout(), agent.getPrefLabelList()));
    Optional.ofNullable(record.getConceptList()).stream().flatMap(Collection::stream)
        .forEach(concept -> result.put(concept.getAbout(),
            Optional.ofNullable(concept.getChoiceList()).stream().flatMap(Collection::stream)
                .filter(Concept.Choice::ifPrefLabel).map(Concept.Choice::getPrefLabel)
                .filter(Objects::nonNull).toList()));
    Optional.ofNullable(record.getOrganizationList()).stream().flatMap(Collection::stream)
        .forEach(organization -> result.put(organization.getAbout(), organization.getPrefLabelList()));
    Optional.ofNullable(record.getPlaceList()).stream().flatMap(Collection::stream)
        .forEach(place -> result.put(place.getAbout(), place.getPrefLabelList()));
    Optional.ofNullable(record.getTimeSpanList()).stream().flatMap(Collection::stream)
        .forEach(timespan -> result.put(timespan.getAbout(), timespan.getPrefLabelList()));
    return result;
  }

  /**
   * Gets descriptions and language from rdf.
   *
   * @param rdf the rdf
   * @param recordId the record id
   * @return the descriptions and language from rdf
   */
  private List<DeBiasInputRecord> getDescriptionsAndLanguageFromRdf(RDF rdf,
      Long recordId,
      String europeanaId) {

    return this.getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifDescription,
        EuropeanaType.Choice::getDescription,
        ResourceOrLiteralType::getString,
        value -> value.getLang().getLang(),
        DeBiasSourceField.DC_DESCRIPTION,
        recordId, europeanaId);
  }

  /**
   * Gets titles and language from rdf.
   *
   * @param rdf the rdf
   * @param recordId the record id
   * @param europeanaId the europeana id
   * @return the titles and language from rdf
   */
  private List<DeBiasInputRecord> getTitlesAndLanguageFromRdf(RDF rdf,
      Long recordId,
      String europeanaId) {

    return this.getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifTitle,
        EuropeanaType.Choice::getTitle,
        LiteralType::getString,
        value -> value.getLang().getLang(),
        DeBiasSourceField.DC_TITLE,
        recordId, europeanaId);
  }

  /**
   * Gets alternative and language from rdf.
   *
   * @param rdf the rdf
   * @param recordId the record id
   * @param europeanaId the europeana id
   * @return the alternative and language from rdf
   */
  private List<DeBiasInputRecord> getAlternativeAndLanguageFromRdf(RDF rdf,
      Long recordId,
      String europeanaId) {

    return this.getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifAlternative,
        EuropeanaType.Choice::getAlternative,
        LiteralType::getString,
        value -> value.getLang().getLang(),
        DeBiasSourceField.DCTERMS_ALTERNATIVE,
        recordId, europeanaId);
  }

  private List<DeBiasInputRecord> getSubjectAndLanguageFromRdf(RDF rdf,
      Long recordId,
      String europeanaId) {
    // this get the string (simple case)
    return this.getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifSubject,
        EuropeanaType.Choice::getSubject,
        ResourceOrLiteralType::getString,
        value -> { if (value.getLang() !=null) return value.getLang().getLang(); else return "";},
        DeBiasSourceField.DC_SUBJECT,
        recordId, europeanaId);
 // this case pointing to an entity
//    return this.getChoicesInStringList(getChoices(rdf),
//        EuropeanaType.Choice::ifSubject,
//        EuropeanaType.Choice::getSubject,
//        res -> res.getResource().getResource(),
//        value -> value.getLang().getLang(),
//        DeBiasSourceField.SKOS_PREFLABEL,
//        recordId, europeanaId);

  }

  private List<DeBiasInputRecord> getTypeAndLanguageFromRdf(RDF rdf,
      Long recordId,
      String europeanaId) {

    return this.getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifSubject,
        EuropeanaType.Choice::getSubject,
        ResourceOrLiteralType::getString,
        value -> { if (value.getLang() !=null) return value.getLang().getLang(); else return "";},
        DeBiasSourceField.DC_TYPE,
        recordId, europeanaId);
  }

  private @NotNull List<Choice> getChoices(RDF rdf) {
    List<ProxyType> providerProxies = this.getProviderProxies(rdf);
    return providerProxies
        .stream()
        .map(EuropeanaType::getChoiceList)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .toList();
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
      Function<T, String> getString, Function<T, String> getLanguage, DeBiasSourceField sourceField,
      Long recordId,
      String europeanaId) {
    return choices.stream()
                  .filter(Objects::nonNull)
                  .filter(choicePredicate)
                  .map(choiceGetter)
                  .map(value -> Optional
                      .ofNullable(DeBiasSupportedLanguage.match(getLanguage.apply(value)))
                      .map(lang -> new DeBiasInputRecord(recordId, europeanaId, getString.apply(value), lang, sourceField))
                      .orElse(null))
                  .filter(Objects::nonNull)
                  .toList();
  }

  /**
   * The type DeBias input record.
   */
  record DeBiasInputRecord(Long recordId,
                           String europeanaId,
                           String literal,
                           DeBiasSupportedLanguage language,
                           DeBiasSourceField sourceField) {

  }

  /**
   * The type DeBias report row.
   */
  public record DeBiasReportRow(Long recordId,
                                String europeanaId,
                                ValueDetection valueDetection,
                                DeBiasSourceField sourceField) {

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
