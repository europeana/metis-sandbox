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
 * Entity representing a transform XSLT file and its type.
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

  /**
   * Constructor.
   *
   * @param datasetId the identifier of the dataset associated with the transformation
   * @param type the XSLT type defining the nature of the transformation
   * @param transformXslt the XSLT content used for the transformation
   */
  public TransformXsltEntity(String datasetId, XsltType type, String transformXslt) {
    this.datasetId = datasetId;
    this.type = type;
    this.transformXslt = transformXslt;
  }

  /**
   * Constructor.
   *
   * @param xsltType the XSLT type defining the nature of the transformation
   * @param newTransformXslt the XSLT content used for the transformation
   */
  public TransformXsltEntity(XsltType xsltType, String newTransformXslt) {
    this.type = xsltType;
    this.transformXslt = newTransformXslt;
  }
}
