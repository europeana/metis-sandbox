package eu.europeana.metis.sandbox.service.metrics;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;

@JsonInclude(Include.NON_EMPTY)
public class DatasetMetrics {

  private Map<String, Object> datasetMetricsMap;

  @JsonAnyGetter
  public Map<String, Object> getDatasetMetricsMap() {
    return datasetMetricsMap;
  }

  @JsonAnySetter
  public void setDatasetMetricsMap(Map<String, Object> datasetMetricsMap) {
    this.datasetMetricsMap = datasetMetricsMap;
  }
}
