package eu.europeana.metis.sandbox.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity that maps default xslt transform table
 */

@Entity
@Table(name = "transform_xslt")
public class TransformXsltEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String datasetId;

  @Column(columnDefinition="TEXT")
  private String transformXslt;

  @Enumerated(EnumType.STRING)
  private XsltType type;


  public TransformXsltEntity(String datasetId, String transformXslt, XsltType type) {
    this.datasetId = datasetId;
    this.transformXslt = transformXslt;
    this.type = type;
  }

  public TransformXsltEntity() {
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public String getTransformXslt() {
    return transformXslt;
  }

  public void setTransformXslt(String transformXslt) {
    this.transformXslt = transformXslt;
  }

  public XsltType getType() {
    return type;
  }

  public void setType(XsltType type) {
    this.type = type;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }
}
