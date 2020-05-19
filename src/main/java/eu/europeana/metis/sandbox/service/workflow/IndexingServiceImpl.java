package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.common.Index;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.io.IOException;
import java.util.Date;
import javax.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
class IndexingServiceImpl implements IndexingService {

  private final Indexer previewIndexer;
  private final Indexer publishIndexer;

  public IndexingServiceImpl(
      Indexer previewIndexer, Indexer publishIndexer) {
    this.previewIndexer = previewIndexer;
    this.publishIndexer = publishIndexer;
  }

  @Override
  public RecordInfo index(Record record, Index index) {
    requireNonNull(record, "Record must not be null");
    requireNonNull(index, "Index must not be null");

    Indexer indexer = Index.PREVIEW == index ? previewIndexer : publishIndexer;
    try {
      indexer.index(record.getContentInputStream(), new Date(), false, null, false);
    } catch (IndexingException ex) {
      throw new RecordProcessingException(record.getRecordId(), ex);
    }

    return new RecordInfo(record);
  }

  @PreDestroy
  public void destroy() throws IOException {
    previewIndexer.close();
    publishIndexer.close();
  }
}
