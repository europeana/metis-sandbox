package eu.europeana.metis.sandbox.dto.report.tier;

import java.util.List;

class EnablingElements {

  private int distinctEnablingElements;
  private List<String> distinctEnablingElementsList;
  private int metadataGroups;
  private List<String> metadataGroupsList;
  // TODO: 22/12/2021 For setting this check QualityAnnotationSolrCreator.SolrTier.
  // TODO: 22/12/2021 Probably extract this method to reuse it here.
  private String metadataTier;

  public EnablingElements() {
  }

  public int getDistinctEnablingElements() {
    return distinctEnablingElements;
  }

  public void setDistinctEnablingElements(int distinctEnablingElements) {
    this.distinctEnablingElements = distinctEnablingElements;
  }

  public List<String> getDistinctEnablingElementsList() {
    return distinctEnablingElementsList;
  }

  public void setDistinctEnablingElementsList(List<String> distinctEnablingElementsList) {
    this.distinctEnablingElementsList = distinctEnablingElementsList;
  }

  public int getMetadataGroups() {
    return metadataGroups;
  }

  public void setMetadataGroups(int metadataGroups) {
    this.metadataGroups = metadataGroups;
  }

  public List<String> getMetadataGroupsList() {
    return metadataGroupsList;
  }

  public void setMetadataGroupsList(List<String> metadataGroupsList) {
    this.metadataGroupsList = metadataGroupsList;
  }

  public String getMetadataTier() {
    return metadataTier;
  }

  public void setMetadataTier(String metadataTier) {
    this.metadataTier = metadataTier;
  }
}
