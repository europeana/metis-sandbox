package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetRemoveServiceImplTest {

  @Mock
  private DatasetService datasetService;

  @Mock
  private RecordLogService recordLogService;

  @Mock
  private IndexingService indexingService;

  @Mock
  private ThumbnailStoreService thumbnailStoreService;

  @InjectMocks
  private DatasetRemoveServiceImpl service;

  @Test
  void remove_expectSucess() {
  }
}