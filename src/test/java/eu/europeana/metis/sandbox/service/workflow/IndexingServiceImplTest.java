package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndexingServiceImplTest {

  @Mock
  private Indexer indexer;

  @InjectMocks
  private IndexingServiceImpl service;

  @Test
  void index_expectSuccess() throws IndexingException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    service.index(record);
    verify(indexer).index(any(InputStream.class), any(Date.class), anyBoolean(), eq(null), anyBoolean());
  }

  @Test
  void index_IndexingIssue_expectFail() throws IndexingException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    doThrow(new IndexerRelatedIndexingException("Failed"))
        .when(indexer).index(any(InputStream.class), any(Date.class), anyBoolean(), eq(null), anyBoolean());
    assertThrows(RecordProcessingException.class, () -> service.index(record));
  }

  @Test
  void index_inputNull_expectFail() {
    assertThrows(NullPointerException.class, () -> service.index(null));
  }

  @Test
  void remove_expectSuccess() {

  }

  @Test
  void destroy_expectSuccess() throws IOException {
    service.destroy();
    verify(indexer).close();
  }
}