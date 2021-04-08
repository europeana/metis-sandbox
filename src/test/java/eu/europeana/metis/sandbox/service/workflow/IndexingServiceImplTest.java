package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.common.IndexEnvironment;
import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndexingServiceImplTest {

  @Mock
  private Indexer previewIndexer;

  @Mock
  private Indexer publishIndexer;

  private IndexingServiceImpl service;

  @BeforeEach
  void init() {
    service = new IndexingServiceImpl(previewIndexer, publishIndexer);
  }

  @Test
  void indexPreview_expectSuccess() throws IndexingException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    service.index(record, IndexEnvironment.PREVIEW);
    verify(previewIndexer).index(any(InputStream.class), any());
  }

  @Test
  void indexPreview_IndexingIssue_expectFail() throws IndexingException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    doThrow(new IndexerRelatedIndexingException("Failed"))
        .when(previewIndexer).index(any(InputStream.class), any());
    assertThrows(RecordProcessingException.class,
        () -> service.index(record, IndexEnvironment.PREVIEW));
  }

  @Test
  void indexPublish_expectSuccess() throws IndexingException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    service.index(record, IndexEnvironment.PUBLISH);
    verify(publishIndexer).index(any(InputStream.class), any());
  }

  @Test
  void indexPublish_IndexingIssue_expectFail() throws IndexingException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    doThrow(new IndexerRelatedIndexingException("Failed"))
        .when(publishIndexer).index(any(InputStream.class), any());
    assertThrows(RecordProcessingException.class,
        () -> service.index(record, IndexEnvironment.PUBLISH));
  }

  @Test
  void index_inputNull_expectFail() {
    assertThrows(NullPointerException.class, () -> service.index(null, IndexEnvironment.PUBLISH));
  }

  @Test
  void remove_expectSuccess() throws IndexingException {
    service.remove("1");

    verify(previewIndexer).removeAll("1", null);
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
    verify(previewIndexer).close();
    verify(publishIndexer).close();
  }
}