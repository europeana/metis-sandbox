package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessServiceImpl.DeBiasInputRecord;
import eu.europeana.metis.schema.jibx.Concept;
import eu.europeana.metis.schema.jibx.EuropeanaType;
import eu.europeana.metis.schema.jibx.EuropeanaType.Choice;
import eu.europeana.metis.schema.jibx.LiteralType;
import eu.europeana.metis.schema.jibx.PrefLabel;
import eu.europeana.metis.schema.jibx.ProxyType;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.metis.schema.jibx.ResourceOrLiteralType;
import eu.europeana.metis.schema.jibx.ResourceOrLiteralType.Resource;
import java.util.ArrayList;
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

  private final RDF rdf;
  private final Record recordToProcess;

  /**
   * Instantiates a new DeBias rdf info extractor.
   *
   * @param rdf the rdf
   * @param recordToProcess the record to process
   */
  public DeBiasRdfInfoExtractor(RDF rdf, Record recordToProcess) {
    this.rdf = rdf;
    this.recordToProcess = recordToProcess;
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
   * Gets choices.
   *
   * @return the choices
   */
  private List<Choice> getChoices() {
    List<ProxyType> providerProxies = getProviderProxies(this.rdf);
    return providerProxies.stream()
                          .map(EuropeanaType::getChoiceList)
                          .filter(Objects::nonNull)
                          .flatMap(Collection::stream)
                          .toList();
  }

  /**
   * Gets literals and languages from rdf.
   *
   * @param <T> the type parameter
   * @param <U> the type parameter
   * @param choicePredicate the choice predicate
   * @param choiceGetter the choice getter
   * @param lookup the lookup
   * @param getString the get string
   * @param getLanguage the get language
   * @param sourceField the source field
   * @return the literals and languages from rdf
   */
  private <T, U> List<DeBiasInputRecord> getLiteralsAndLanguagesFromRdf(
      Predicate<Choice> choicePredicate, Function<Choice, T> choiceGetter,
      Function<T, List<? extends U>> lookup, Function<U, String> getString,
      Function<U, String> getLanguage, DeBiasSourceField sourceField) {
    return getChoices()
        .stream()
        .filter(Objects::nonNull)
        .filter(choicePredicate)
        .map(choiceGetter)
        .filter(Objects::nonNull)
        .map(lookup)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .filter(Objects::nonNull)
        .map(value -> Optional.of(getLanguage.apply(value))
                              .map(DeBiasSupportedLanguage::match)
                              .map(
                                  language -> new DeBiasInputRecord(this.recordToProcess.getRecordId(),
                                      this.recordToProcess.getEuropeanaId(), getString.apply(value),
                                      language, sourceField)).orElse(null))
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Gets descriptions and language from rdf.
   *
   * @return the descriptions and language from rdf
   */
  List<DeBiasInputRecord> getDescriptionsAndLanguageFromRdf() {
    return getLiteralsAndLanguagesFromRdf(
        EuropeanaType.Choice::ifDescription,
        EuropeanaType.Choice::getDescription,
        List::of,
        ResourceOrLiteralType::getString,
        getLanguageResourceOrLiteralType(),
        DeBiasSourceField.DC_DESCRIPTION);
  }

  /**
   * Gets titles and language from rdf.
   *
   * @return the titles and language from rdf
   */
  List<DeBiasInputRecord> getTitlesAndLanguageFromRdf() {
    return getLiteralsAndLanguagesFromRdf(
        EuropeanaType.Choice::ifTitle,
        EuropeanaType.Choice::getTitle,
        List::of,
        LiteralType::getString,
        getLanguageLiteralType(),
        DeBiasSourceField.DC_TITLE);
  }

  /**
   * Gets alternative and language from rdf.
   *
   * @return the alternative and language from rdf
   */
  List<DeBiasInputRecord> getAlternativeAndLanguageFromRdf() {
    return getLiteralsAndLanguagesFromRdf(
        EuropeanaType.Choice::ifAlternative,
        EuropeanaType.Choice::getAlternative,
        List::of,
        LiteralType::getString,
        getLanguageLiteralType(),
        DeBiasSourceField.DCTERMS_ALTERNATIVE);
  }

  /**
   * Gets subject and language from rdf.
   *
   * @return the subject and language from rdf
   */
  List<DeBiasInputRecord> getSubjectAndLanguageFromRdf() {
    return getLiteralsAndLanguagesFromRdf(
        EuropeanaType.Choice::ifSubject,
        EuropeanaType.Choice::getSubject,
        List::of,
        ResourceOrLiteralType::getString,
        getLanguageResourceOrLiteralType(),
        DeBiasSourceField.DC_SUBJECT_LITERAL);
  }

  /**
   * Gets type and language from rdf.
   *
   * @return the type and language from rdf
   */
  List<DeBiasInputRecord> getTypeAndLanguageFromRdf() {
    return getLiteralsAndLanguagesFromRdf(
        EuropeanaType.Choice::ifType,
        EuropeanaType.Choice::getType,
        List::of,
        ResourceOrLiteralType::getString,
        getLanguageResourceOrLiteralType(),
        DeBiasSourceField.DC_TYPE_LITERAL);
  }

  /**
   * Gets subject references and type references from rdf.
   *
   * @return the subject references and type references from rdf
   */
  List<DeBiasInputRecord> getSubjectReferencesAndTypeReferencesFromRdf() {
    Map<String, List<PrefLabel>> contextualClassesLabels = getContextualClassLabelsByRdfAbout();
    List<DeBiasInputRecord> result = new ArrayList<>();
    result.addAll(getReferencesAndLanguageFromRdf(contextualClassesLabels,
        Choice::ifSubject, Choice::getSubject, DeBiasSourceField.DC_SUBJECT_REFERENCE));
    result.addAll(getReferencesAndLanguageFromRdf(contextualClassesLabels,
        Choice::ifType, Choice::getType, DeBiasSourceField.DC_TYPE_REFERENCE));
    return result;
  }

  /**
   * Gets references and language from rdf.
   *
   * @param contextualClassesLabels the contextual classes labels
   * @param choicePredicate the choice predicate
   * @param choiceGetter the choice getter
   * @param sourceField the source field
   * @return the type references and language from rdf
   */
  List<DeBiasInputRecord> getReferencesAndLanguageFromRdf(
      Map<String, List<PrefLabel>> contextualClassesLabels,
      Predicate<Choice> choicePredicate,
      Function<Choice, ResourceOrLiteralType> choiceGetter, DeBiasSourceField sourceField) {
    return getLiteralsAndLanguagesFromRdf(choicePredicate, choiceGetter,
        reference -> Optional.of(reference)
                             .map(ResourceOrLiteralType::getResource)
                             .map(Resource::getResource)
                             .map(contextualClassesLabels::get)
                             .orElse(null),
        LiteralType::getString,
        getLanguageLiteralType(),
        sourceField);
  }

  /**
   * Gets contextual class labels by rdf about.
   *
   * @return the contextual class labels by rdf about
   */
  private Map<String, List<PrefLabel>> getContextualClassLabelsByRdfAbout() {
    final Map<String, List<PrefLabel>> result = new HashMap<>();
    Optional.ofNullable(rdf.getAgentList())
            .stream().flatMap(Collection::stream)
            .forEach(agent -> result.put(agent.getAbout(), agent.getPrefLabelList()));
    Optional.ofNullable(rdf.getConceptList()).stream().flatMap(Collection::stream).forEach(
        concept -> result.put(concept.getAbout(),
            Optional.ofNullable(concept.getChoiceList())
                    .stream().flatMap(Collection::stream).filter(Concept.Choice::ifPrefLabel)
                    .map(Concept.Choice::getPrefLabel).filter(Objects::nonNull).toList()));
    Optional.ofNullable(rdf.getOrganizationList())
            .stream().flatMap(Collection::stream)
            .forEach(organization -> result.put(organization.getAbout(), organization.getPrefLabelList()));
    Optional.ofNullable(rdf.getPlaceList())
            .stream().flatMap(Collection::stream)
            .forEach(place -> result.put(place.getAbout(), place.getPrefLabelList()));
    Optional.ofNullable(rdf.getTimeSpanList())
            .stream().flatMap(Collection::stream)
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
      int endIndex = (partition == partitions) ? sourceList.size() : ((partition + 1) * partitionSize);
      return sourceList.subList(startIndex, endIndex);
    });
  }
}
