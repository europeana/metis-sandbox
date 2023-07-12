package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import java.io.IOException;
import java.io.InputStream;

import eu.europeana.metis.sandbox.service.record.RecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndexingServiceImplTest {

  @Mock
  private Indexer publishIndexer;

  private IndexingServiceImpl service;

  @Mock
  private RecordService recordService;

  @BeforeEach
  void init() {
    service = new IndexingServiceImpl(publishIndexer, recordService);
  }

  @Test
  void indexPublish_expectSuccess() throws IndexingException {
    var record = Record.builder().recordId(1L)
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();
    TierResults mock = new TierResults(MediaTier.T1, MetadataTier.TA);

    when(publishIndexer.indexAndGetTierCalculations(any(InputStream.class), any(IndexingProperties.class)))
            .thenReturn(mock);

    service.index(record);
    verify(publishIndexer).indexAndGetTierCalculations(any(InputStream.class), any());
    verify(recordService).setContentTierAndMetadataTier(record, MediaTier.T1, MetadataTier.TA);
  }

  @Test
  void indexPublish_IndexingIssue_expectFail() throws IndexingException {
    var record = Record.builder().recordId(1L)
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    doThrow(new IndexerRelatedIndexingException("Failed"))
        .when(publishIndexer).indexAndGetTierCalculations(any(InputStream.class), any());
    assertThrows(RecordProcessingException.class,
        () -> service.index(record));
  }

  @Test
  void indexPublish_TierCalculationIssue_expectFail() {
    var record = Record.builder().recordId(1L)
            .content("".getBytes()).language(Language.IT).country(Country.ITALY)
            .datasetName("").datasetId("").build();

    RecordProcessingException exception = assertThrows(RecordProcessingException.class,
            () -> service.index(record));
    assertTrue(exception.getReportMessage().contains("Something went wrong with tier calculations with record"));
  }

  @Test
  void index_inputNull_expectFail() {
    assertThrows(NullPointerException.class, () -> service.index(null));
  }

  @Test
  void remove_expectSuccess() throws IndexingException {
    service.remove("1");
    verify(publishIndexer).removeAll("1", null);
  }

  @Test
  void remove_indexingException_expectFail() throws IndexingException {
    when(publishIndexer.removeAll("1", null))
        .thenThrow(new IndexerRelatedIndexingException("failed"));
    assertThrows(DatasetIndexRemoveException.class, () -> service.remove("1"));
  }

  @Test
  void remove_inputNull_expectFail() {
    assertThrows(NullPointerException.class, () -> service.remove(null));
  }

  @Test
  void destroy_expectSuccess() throws IOException {
    service.destroy();
    verify(publishIndexer).close();
  }
}
