package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.service.debias.DeBiasProcessService.DeBiasInputRecord;
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
 * Extracts and processes RDF information for debiasing purposes.
 */
public class DeBiasRdfInfoExtractor {

  private final RDF rdf;
  private final String recordId;

  /**
   * Constructor.
   *
   * @param rdf the rdf instance for data extraction
   * @param recordId the unique identifier for the record
   */
  public DeBiasRdfInfoExtractor(RDF rdf, String recordId) {
    this.rdf = rdf;
    this.recordId = recordId;
  }

  private static boolean isProviderProxy(ProxyType proxy) {
    return proxy.getEuropeanaProxy() == null || BooleanUtils.isFalse(proxy.getEuropeanaProxy().isEuropeanaProxy());
  }

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

  private List<Choice> getChoices() {
    List<ProxyType> providerProxies = getProviderProxies(this.rdf);
    return providerProxies.stream()
                          .map(EuropeanaType::getChoiceList)
                          .filter(Objects::nonNull)
                          .flatMap(Collection::stream)
                          .toList();
  }

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
                                  language -> new DeBiasInputRecord(
                                      recordId, getString.apply(value),
                                      language, sourceField)).orElse(null))
        .filter(Objects::nonNull)
        .toList();
  }

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
   * Gets descriptions and language from rdf.
   *
   * @return the descriptions and language from rdf
   */
  public List<DeBiasInputRecord> getDescriptionsAndLanguageFromRdf() {
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
  public List<DeBiasInputRecord> getTitlesAndLanguageFromRdf() {
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
  public List<DeBiasInputRecord> getAlternativeAndLanguageFromRdf() {
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
  public List<DeBiasInputRecord> getSubjectAndLanguageFromRdf() {
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
  public List<DeBiasInputRecord> getTypeAndLanguageFromRdf() {
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
  public List<DeBiasInputRecord> getSubjectReferencesAndTypeReferencesFromRdf() {
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
   * Partition list stream.
   *
   * @param <T> the type parameter
   * @param sourceList the source
   * @param partitionSize the batch size
   * @return the stream
   */
  public static <T> Stream<List<T>> partitionList(List<T> sourceList, int partitionSize) {
    if (partitionSize <= 0) {
      throw new IllegalArgumentException(
          String.format("The partition size cannot be smaller than or equal 0. Actual value: %s", partitionSize));
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
