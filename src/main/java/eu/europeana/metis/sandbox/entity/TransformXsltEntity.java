package eu.europeana.metis.sandbox.entity;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity that maps default xslt transform table
 */

@Entity
@Table(name = "transform_xslt")
public class TransformXsltEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String transformXslt;


  public TransformXsltEntity(String transformXslt) {
    this.transformXslt = transformXslt;
  }

  public TransformXsltEntity() {
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
}
