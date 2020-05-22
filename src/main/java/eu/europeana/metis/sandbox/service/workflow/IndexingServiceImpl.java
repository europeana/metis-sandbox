package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.io.IOException;
import java.util.Date;
import javax.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
class IndexingServiceImpl implements IndexingService {

  private final Indexer indexer;

  public IndexingServiceImpl(
      Indexer indexer) {
    this.indexer = indexer;
  }

  @Override
  public RecordInfo index(Record record) {
    requireNonNull(record, "Record must not be null");

    try {
      indexer.index(record.getContentInputStream(), new Date(), false, null, false);
    } catch (IndexingException ex) {
      throw new RecordProcessingException(record.getRecordId(), ex);
    }

    return new RecordInfo(record);
  }

  @Override
  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");

    try {
      indexer.removeAll(datasetId, null);
      //publishIndexer.removeAll(dataset, null);
    } catch (IndexingException e) {
      throw new DatasetIndexRemoveException(datasetId, e);
    }
  }

  @PreDestroy
  public void destroy() throws IOException {
    indexer.close();
  }
}
