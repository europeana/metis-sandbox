package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.service.debias.DeBiasRdfInfoExtractor;
import eu.europeana.metis.sandbox.service.debias.DeBiasRdfInfoExtractor.DeBiasInputRecord;
import eu.europeana.metis.sandbox.service.debias.DeBiasSourceField;
import eu.europeana.metis.sandbox.service.debias.DeBiasSupportedLanguage;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.jibx.RDF;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeBiasRdfInfoExtractorTest {

  DeBiasRdfInfoExtractor extractor;

  @BeforeEach
  void setUp() throws Exception {
    String testRecordContent = new TestUtils().readFileToString(
        "record" + File.separator + "debias" + File.separator + "debias-video-record.xml");
    RDF rdf = new RdfConversionUtils().convertStringToRdf(testRecordContent);

    extractor = new DeBiasRdfInfoExtractor(rdf, "recordId");
  }

  @Test
  void getDescriptionsAndLanguageFromRdf() {
    var expected = Set.of(
        "Interactive quicktime panorama of link path 1-5.Skara Brae is an archaeological site ",
        "Interaktives Quicktime-Panorama des Verbindungspfads 1-5. Skara Brae ist eine archäol",
        "Panorama quicktime interattivo del percorso di collegamento 1-5. Skara Brae è un sito",
        "Interactief quicktime panorama van linkpad 1-5. Skara Brae is een archeologische vind",
        "Panorama interactif en temps réel du chemin de liaison 1-5. Skara Brae est un site ar");
    List<DeBiasInputRecord> list = extractor.getDescriptionsAndLanguageFromRdf();
    assertNotNull(list);
    list.forEach(item -> assertEquals(DeBiasSourceField.DC_DESCRIPTION, item.sourceField()));
    assertEquals(Set.of("nl", "de", "it", "fr", "en"), list.stream()
                                                           .map(DeBiasRdfInfoExtractor.DeBiasInputRecord::language)
                                                           .map(DeBiasSupportedLanguage::getCodeISO6391)
                                                           .collect(Collectors.toSet()));
    assertEquals(expected, list.stream()
                               .map(DeBiasRdfInfoExtractor.DeBiasInputRecord::literal)
                               .map(item -> item.subSequence(0, 85))
                               .collect(Collectors.toSet())
    );
  }

  @Test
  void getTitlesAndLanguageFromRdf() {
    var expected = Set.of("Panorama Movie of link path 1-5, Skara Brae addict and aboriginal",
        "Panoramafilm des Verbindungspfads 1-5, Skara Brae, Süchtiger und Ureinwohner",
        "Film panoramico del percorso di collegamento 1-5, Skara Brae, tossicodipendente e aborigeno",
        "Panoramafilm van linkpad 1-5, Skara Brae, verslaafde en aboriginal",
        "Film panoramique du chemin de liaison 1-5, Skara Brae, toxicomane et aborigène");
    List<DeBiasInputRecord> list = extractor.getTitlesAndLanguageFromRdf();
    assertNotNull(list);
    list.forEach(item -> assertEquals(DeBiasSourceField.DC_TITLE, item.sourceField()));
    assertEquals(Set.of("nl", "de", "it", "fr", "en"), list.stream()
                                                           .map(DeBiasRdfInfoExtractor.DeBiasInputRecord::language)
                                                           .map(DeBiasSupportedLanguage::getCodeISO6391)
                                                           .collect(Collectors.toSet()));
    assertEquals(expected, list.stream()
                               .map(DeBiasRdfInfoExtractor.DeBiasInputRecord::literal)
                               .collect(Collectors.toSet()));
  }

  @Test
  void getAlternativeAndLanguageFromRdf() {
    List<DeBiasInputRecord> list = extractor.getAlternativeAndLanguageFromRdf();
    assertNotNull(list);
    assertTrue(list.isEmpty());
  }

  @Test
  void getSubjectAndLanguageFromRdf() {
    var expected = Set.of("Settlement clans", "Clans de la colonie", "Clan di insediamento", "Siedlungsclans");
    List<DeBiasInputRecord> list = extractor.getSubjectAndLanguageFromRdf();
    assertNotNull(list);
    list.forEach(item -> assertEquals(DeBiasSourceField.DC_SUBJECT_LITERAL, item.sourceField()));
    assertEquals(Set.of("nl", "de", "it", "fr", "en"), list.stream()
                                                           .map(DeBiasRdfInfoExtractor.DeBiasInputRecord::language)
                                                           .map(DeBiasSupportedLanguage::getCodeISO6391)
                                                           .collect(Collectors.toSet()));
    assertEquals(expected, list.stream()
                               .map(DeBiasRdfInfoExtractor.DeBiasInputRecord::literal)
                               .collect(Collectors.toSet()));
  }

  @Test
  void getTypeAndLanguageFromRdf() {
    var expected = Set.of("Movie housewife, guy, chairman, addict, victims, homemaker",
        "Femme au foyer, homme, président, toxicomane, victimes, femme au foyer",
        "Filmhuisvrouw, man, voorzitter, verslaafde, slachtoffers, huisvrouw",
        "Casalinga del cinema, ragazzo, presidente, tossicodipendente, vittime, casalinga",
        "Filmhausfrau, Typ, Vorsitzender, Süchtiger, Opfer, Hausfrau");
    List<DeBiasInputRecord> list = extractor.getTypeAndLanguageFromRdf();
    assertNotNull(list);
    list.forEach(item -> assertEquals(DeBiasSourceField.DC_TYPE_LITERAL, item.sourceField()));
    assertEquals(Set.of("nl", "de", "it", "fr", "en"), list.stream()
                                                           .map(DeBiasRdfInfoExtractor.DeBiasInputRecord::language)
                                                           .map(DeBiasSupportedLanguage::getCodeISO6391)
                                                           .collect(Collectors.toSet()));
    assertEquals(expected, list.stream()
                               .map(DeBiasRdfInfoExtractor.DeBiasInputRecord::literal)
                               .collect(Collectors.toSet()));
  }

  @Test
  void getSubjectReferencesAndTypeReferencesFromRdf() {
    List<DeBiasInputRecord> list = extractor.getSubjectReferencesAndTypeReferencesFromRdf();
    assertNotNull(list);
    assertTrue(list.isEmpty());
  }

  @Test
  void partitionList() {
    List<String> testPartition = List.of("A", "B", "C", "D", "E", "F", "G", "H");
    Set<List<String>> expectedPartitions = Set.of(List.of("A", "B"), List.of("C", "D"),
        List.of("E", "F"), List.of("G", "H"));
    Stream<List<String>> listStream = DeBiasRdfInfoExtractor.partitionList(testPartition, 2);
    assertNotNull(listStream);
    listStream.forEach(item -> assertTrue(expectedPartitions.contains(item)));
  }

  @Test
  void partitionListEmpty() {
    Stream<List<String>> listStream = DeBiasRdfInfoExtractor.partitionList(List.of(), 2);
    assertNotNull(listStream);
    assertTrue(listStream.toList().isEmpty());
  }

  @Test
  void partitionListError() {
    Supplier<Object> makePartition = () -> DeBiasRdfInfoExtractor.partitionList(List.of(), -2);
    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, makePartition::get);
    assertNotNull(illegalArgumentException);
    assertEquals("The partition size cannot be smaller than or equal 0. Actual value: -2", illegalArgumentException.getMessage());
  }
}
