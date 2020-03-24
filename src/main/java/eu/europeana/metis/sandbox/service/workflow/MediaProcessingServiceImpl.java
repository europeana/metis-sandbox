package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import org.springframework.stereotype.Service;

@Service
class MediaProcessingServiceImpl implements MediaProcessingService {

  @Override
  public Record processMedia(Record record) {
    return record;
  }
}
