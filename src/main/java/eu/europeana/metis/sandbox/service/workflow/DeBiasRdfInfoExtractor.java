package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessServiceImpl.DeBiasInputRecord;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;

/**
 * The type DeBias rdf info extractor.
 */
public class DeBiasRdfInfoExtractor {

  private DeBiasRdfInfoExtractor() {
    // not instantiable class
  }

  /**
   * Gets choices.
   *
   * @param rdf the rdf
   * @return the choices
   */
  private static List<Choice> getChoices(RDF rdf) {
    List<ProxyType> providerProxies = getProviderProxies(rdf);
    return providerProxies.stream()
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
  private static boolean isProviderProxy(ProxyType proxy) {
    return proxy.getEuropeanaProxy() == null || BooleanUtils.isFalse(proxy.getEuropeanaProxy().isEuropeanaProxy());
  }

  /**
   * Gets provider proxies.
   *
   * @param rdf the rdf
   * @return the provider proxies
   */
  private static List<ProxyType> getProviderProxies(RDF rdf) {
    return Optional.ofNullable(rdf.getProxyList()).stream().flatMap(Collection::stream).filter(Objects::nonNull)
                   .filter(DeBiasRdfInfoExtractor::isProviderProxy).toList();
  }

  private static Function<LiteralType, String> getLanguageLiteralType() {
    return value -> Optional.ofNullable(value.getLang())
                            .map(LiteralType.Lang::getLang)
                            .orElse("");
  }

  private static Function<ResourceOrLiteralType, String> getLanguageResourceOrLiteralType() {
    return value -> Optional.ofNullable(value.getLang())
                            .map(ResourceOrLiteralType.Lang::getLang)
                            .orElse("");
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
   * @param sourceField the source field
   * @param recordInfo the record info
   * @return the choices in string list
   */
  private static <T> List<DeBiasInputRecord> getChoicesInStringList(List<EuropeanaType.Choice> choices,
      Predicate<Choice> choicePredicate, Function<Choice, T> choiceGetter, Function<T, String> getString,
      Function<T, String> getLanguage, DeBiasSourceField sourceField, Record recordInfo) {
    return choices.stream()
                  .filter(Objects::nonNull)
                  .filter(choicePredicate)
                  .map(choiceGetter)
                  .map(value -> Optional.ofNullable(DeBiasSupportedLanguage.match(getLanguage.apply(value)))
                                        .map(language -> new DeBiasInputRecord(recordInfo.getRecordId(),
                                            recordInfo.getEuropeanaId(), getString.apply(value),
                                            language, sourceField)).orElse(null))
                  .filter(Objects::nonNull)
                  .toList();
  }

  /**
   * Gets descriptions and language from rdf.
   *
   * @param rdf the rdf
   * @param recordInfo the record info
   * @return the descriptions and language from rdf
   */
  static List<DeBiasInputRecord> getDescriptionsAndLanguageFromRdf(RDF rdf, Record recordInfo) {
    return getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifDescription,
        EuropeanaType.Choice::getDescription,
        ResourceOrLiteralType::getString,
        getLanguageResourceOrLiteralType(),
        DeBiasSourceField.DC_DESCRIPTION,
        recordInfo);
  }

  /**
   * Gets titles and language from rdf.
   *
   * @param rdf the rdf
   * @param recordInfo the record info
   * @return the titles and language from rdf
   */
  static List<DeBiasInputRecord> getTitlesAndLanguageFromRdf(RDF rdf, Record recordInfo) {
    return getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifTitle,
        EuropeanaType.Choice::getTitle,
        LiteralType::getString,
        getLanguageLiteralType(),
        DeBiasSourceField.DC_TITLE,
        recordInfo);
  }

  /**
   * Gets alternative and language from rdf.
   *
   * @param rdf the rdf
   * @param recordInfo the record info
   * @return the alternative and language from rdf
   */
  static List<DeBiasInputRecord> getAlternativeAndLanguageFromRdf(RDF rdf, Record recordInfo) {
    return getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifAlternative,
        EuropeanaType.Choice::getAlternative,
        LiteralType::getString,
        getLanguageLiteralType(),
        DeBiasSourceField.DCTERMS_ALTERNATIVE,
        recordInfo);
  }

  /**
   * Gets subject and language from rdf.
   *
   * @param rdf the rdf
   * @param recordInfo the record info
   * @return the subject and language from rdf
   */
  static List<DeBiasInputRecord> getSubjectAndLanguageFromRdf(RDF rdf, Record recordInfo) {
    return getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifSubject,
        EuropeanaType.Choice::getSubject,
        ResourceOrLiteralType::getString,
        getLanguageResourceOrLiteralType(),
        DeBiasSourceField.DC_SUBJECT,
        recordInfo);
  }

  /**
   * Gets type and language from rdf.
   *
   * @param rdf the rdf
   * @param recordInfo the record info
   * @return the type and language from rdf
   */
  static List<DeBiasInputRecord> getTypeAndLanguageFromRdf(RDF rdf, Record recordInfo) {
    return getChoicesInStringList(getChoices(rdf),
        EuropeanaType.Choice::ifType,
        EuropeanaType.Choice::getType,
        ResourceOrLiteralType::getString,
        getLanguageResourceOrLiteralType(),
        DeBiasSourceField.DC_TYPE, recordInfo);
  }

  /**
   * Gets type references and language from rdf.
   *
   * @param recordToProcess the record to process
   * @param rdf the rdf
   * @param contextualClassesLabels the contextual classes labels
   * @return the type references and language from rdf
   */
  static List<DeBiasInputRecord> getTypeReferencesAndLanguageFromRdf(Record recordToProcess,
      RDF rdf,
      Map<String, List<PrefLabel>> contextualClassesLabels) {
    return getChoices(rdf).stream()
                          .filter(Choice::ifType)
                          .map(Choice::getType)
                          .filter(Objects::nonNull)
                          .map(ResourceOrLiteralType::getResource)
                          .filter(Objects::nonNull)
                          .map(Resource::getResource)
                          .filter(Objects::nonNull)
                          .map(contextualClassesLabels::get)
                          .filter(Objects::nonNull)
                          .flatMap(Collection::stream)
                          .map(prefLabel -> Optional.ofNullable(prefLabel.getLang())
                                                    .map(Lang::getLang)
                                                    .map(DeBiasSupportedLanguage::match)
                                                    .map(
                                                        language -> new DeBiasInputRecord(
                                                            recordToProcess.getRecordId(), recordToProcess.getEuropeanaId(),
                                                            prefLabel.getString(), language, DeBiasSourceField.DC_TYPE))
                                                    .orElse(null))
                          .filter(Objects::nonNull)
                          .toList();
  }

  /**
   * Gets subject references and language from rdf.
   *
   * @param recordToProcess the record to process
   * @param rdf the rdf
   * @param contextualClassesLabels the contextual classes labels
   * @return the subject references and language from rdf
   */
  static List<DeBiasInputRecord> getSubjectReferencesAndLanguageFromRdf(eu.europeana.metis.sandbox.domain.Record recordToProcess,
      RDF rdf,
      Map<String, List<PrefLabel>> contextualClassesLabels) {
    return getChoices(rdf).stream()
                          .filter(Choice::ifSubject)
                          .map(Choice::getSubject)
                          .filter(Objects::nonNull)
                          .map(ResourceOrLiteralType::getResource)
                          .filter(Objects::nonNull)
                          .map(Resource::getResource)
                          .filter(Objects::nonNull)
                          .map(contextualClassesLabels::get)
                          .filter(Objects::nonNull)
                          .flatMap(Collection::stream)
                          .map(
                              prefLabel -> Optional.ofNullable(prefLabel.getLang())
                                                   .map(Lang::getLang)
                                                   .map(DeBiasSupportedLanguage::match)
                                                   .map(
                                                       language -> new DeBiasInputRecord(
                                                           recordToProcess.getRecordId(), recordToProcess.getEuropeanaId(),
                                                           prefLabel.getString(), language, DeBiasSourceField.DC_SUBJECT))
                                                   .orElse(null))
                          .filter(Objects::nonNull)
                          .toList();
  }

  /**
   * Gets contextual class labels by rdf about.
   *
   * @param rdf the rdf
   * @return the contextual class labels by rdf about
   */
  static Map<String, List<PrefLabel>> getContextualClassLabelsByRdfAbout(RDF rdf) {
    final Map<String, List<PrefLabel>> result = new HashMap<>();
    Optional.ofNullable(rdf.getAgentList()).stream().flatMap(Collection::stream)
            .forEach(agent -> result.put(agent.getAbout(), agent.getPrefLabelList()));
    Optional.ofNullable(rdf.getConceptList()).stream().flatMap(Collection::stream).forEach(
        concept -> result.put(concept.getAbout(),
            Optional.ofNullable(concept.getChoiceList()).stream().flatMap(Collection::stream).filter(Concept.Choice::ifPrefLabel)
                    .map(Concept.Choice::getPrefLabel).filter(Objects::nonNull).toList()));
    Optional.ofNullable(rdf.getOrganizationList()).stream().flatMap(Collection::stream)
            .forEach(organization -> result.put(organization.getAbout(), organization.getPrefLabelList()));
    Optional.ofNullable(rdf.getPlaceList()).stream().flatMap(Collection::stream)
            .forEach(place -> result.put(place.getAbout(), place.getPrefLabelList()));
    Optional.ofNullable(rdf.getTimeSpanList()).stream().flatMap(Collection::stream)
            .forEach(timespan -> result.put(timespan.getAbout(), timespan.getPrefLabelList()));
    return result;
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
    return IntStream.rangeClosed(0, partitions).mapToObj(partition -> {
      int startIndex = partition * partitionSize;
      int endIndex = (partition == partitions) ? sourceList.size() : (partition + 1) * partitionSize;
      return sourceList.subList(startIndex, endIndex);
    });
  }
}
