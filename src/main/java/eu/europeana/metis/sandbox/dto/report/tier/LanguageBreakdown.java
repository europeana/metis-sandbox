package eu.europeana.metis.sandbox.dto.report.tier;

import java.util.List;

class LanguageBreakdown {

  private int potentialLanguageQualifiedElements;
  private int actualLanguageQualifiedElements;
  private int actualLanguageQualifiedElementsPercentage;
  private int actualLanguageUnqualifiedElements;
  private List<String> actualLanguageUnqualifiedElementsList;
  // TODO: 22/12/2021 For setting this check QualityAnnotationSolrCreator.SolrTier.
  // TODO: 22/12/2021 Probably extract this method to reuse it here.
  private String metadataTier;

  public LanguageBreakdown() {
  }

  public int getPotentialLanguageQualifiedElements() {
    return potentialLanguageQualifiedElements;
  }

  public void setPotentialLanguageQualifiedElements(int potentialLanguageQualifiedElements) {
    this.potentialLanguageQualifiedElements = potentialLanguageQualifiedElements;
  }

  public int getActualLanguageQualifiedElements() {
    return actualLanguageQualifiedElements;
  }

  public void setActualLanguageQualifiedElements(int actualLanguageQualifiedElements) {
    this.actualLanguageQualifiedElements = actualLanguageQualifiedElements;
  }

  public int getActualLanguageQualifiedElementsPercentage() {
    return actualLanguageQualifiedElementsPercentage;
  }

  public void setActualLanguageQualifiedElementsPercentage(int actualLanguageQualifiedElementsPercentage) {
    this.actualLanguageQualifiedElementsPercentage = actualLanguageQualifiedElementsPercentage;
  }

  public int getActualLanguageUnqualifiedElements() {
    return actualLanguageUnqualifiedElements;
  }

  public void setActualLanguageUnqualifiedElements(int actualLanguageUnqualifiedElements) {
    this.actualLanguageUnqualifiedElements = actualLanguageUnqualifiedElements;
  }

  public List<String> getActualLanguageUnqualifiedElementsList() {
    return actualLanguageUnqualifiedElementsList;
  }

  public void setActualLanguageUnqualifiedElementsList(List<String> actualLanguageUnqualifiedElementsList) {
    this.actualLanguageUnqualifiedElementsList = actualLanguageUnqualifiedElementsList;
  }

  public String getMetadataTier() {
    return metadataTier;
  }

  public void setMetadataTier(String metadataTier) {
    this.metadataTier = metadataTier;
  }
}
