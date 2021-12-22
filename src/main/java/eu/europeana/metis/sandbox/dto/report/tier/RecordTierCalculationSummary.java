package eu.europeana.metis.sandbox.dto.report.tier;

class RecordTierCalculationSummary {

  private String europeanaRecordId;
  private String providerRecordId;
  private String contentTier;
  // TODO: 22/12/2021 For setting this check QualityAnnotationSolrCreator.SolrTier.
  // TODO: 22/12/2021 Probably extract this method to reuse it here.
  private String metadataTier;
  private String portalLink;
  private String harvestedRecordLink;

  public RecordTierCalculationSummary() {
  }

  public String getEuropeanaRecordId() {
    return europeanaRecordId;
  }

  public void setEuropeanaRecordId(String europeanaRecordId) {
    this.europeanaRecordId = europeanaRecordId;
  }

  public String getProviderRecordId() {
    return providerRecordId;
  }

  public void setProviderRecordId(String providerRecordId) {
    this.providerRecordId = providerRecordId;
  }

  public String getContentTier() {
    return contentTier;
  }

  public void setContentTier(String contentTier) {
    this.contentTier = contentTier;
  }

  public String getMetadataTier() {
    return metadataTier;
  }

  public void setMetadataTier(String metadataTier) {
    this.metadataTier = metadataTier;
  }

  public String getPortalLink() {
    return portalLink;
  }

  public void setPortalLink(String portalLink) {
    this.portalLink = portalLink;
  }

  public String getHarvestedRecordLink() {
    return harvestedRecordLink;
  }

  public void setHarvestedRecordLink(String harvestedRecordLink) {
    this.harvestedRecordLink = harvestedRecordLink;
  }
}
