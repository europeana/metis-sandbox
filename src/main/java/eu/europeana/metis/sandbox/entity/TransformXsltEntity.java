package eu.europeana.metis.sandbox.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity that maps default xslt transform table
 */

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transform_xslt")
public class TransformXsltEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String datasetId;

  @Enumerated(EnumType.STRING)
  private XsltType type;

  @Column(columnDefinition="TEXT", nullable = false)
  private String transformXslt;

  public TransformXsltEntity(String datasetId, XsltType type, String transformXslt) {
    this.datasetId = datasetId;
    this.type = type;
    this.transformXslt = transformXslt;
  }

  public TransformXsltEntity(XsltType xsltType, String newTransformXslt) {
    this.type = xsltType;
    this.transformXslt = newTransformXslt;
  }
}
