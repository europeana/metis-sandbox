package eu.europeana.metis.sandbox.common;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HarvestContent {

  private AtomicBoolean recordLimitExceeded;
  private List<ByteArrayInputStream> content;

  public HarvestContent(AtomicBoolean recordLimitExceeded,
      List<ByteArrayInputStream> content) {
    this.recordLimitExceeded = recordLimitExceeded;
    this.content = content;
  }

  public boolean hasReachedRecordLimit() {
    return recordLimitExceeded.get();
  }

  public List<ByteArrayInputStream> getContent() {
    return content;
  }

  public void setContent(List<ByteArrayInputStream> content) {
    this.content = content;
  }
}
