package eu.europeana.metis.sandbox.service.debias;

public class Inquiry {

  private int datasetId;
  private State state;

  public Inquiry(int datasetId, State state) {
    this.datasetId = datasetId;
    this.state = state;
  }

  @Override
  public String toString() {
    return "Inquiry [id=" + datasetId + ", state=" + state + "]";
  }

  public int getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(int datasetId) {
    this.datasetId = datasetId;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }
}
