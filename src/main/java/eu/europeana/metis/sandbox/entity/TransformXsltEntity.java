package eu.europeana.metis.sandbox.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity that maps default xslt transform table
 */

@Entity
@Table(name = "default_transform_xslt")
public class TransformXsltEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(columnDefinition="TEXT")
  private String transformXslt;


  public TransformXsltEntity(String transformXslt) {
    this.transformXslt = transformXslt;
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
}
