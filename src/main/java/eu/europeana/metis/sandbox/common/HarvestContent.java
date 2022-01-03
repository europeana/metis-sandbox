package eu.europeana.metis.sandbox.common;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HarvestContent {

  private AtomicBoolean reachedRecordLimit;
  private List<ByteArrayInputStream> content;

  public HarvestContent(AtomicBoolean reachedRecordLimit,
      List<ByteArrayInputStream> content) {
    this.reachedRecordLimit = reachedRecordLimit;
    this.content = content;
  }

  public boolean hasReachedRecordLimit() {
    return reachedRecordLimit.get();
  }

  public void setReachedRecordLimit(AtomicBoolean reachedRecordLimit) {
    this.reachedRecordLimit = reachedRecordLimit;
  }

  public List<ByteArrayInputStream> getContent() {
    return content;
  }

  public void setContent(List<ByteArrayInputStream> content) {
    this.content = content;
  }
}
