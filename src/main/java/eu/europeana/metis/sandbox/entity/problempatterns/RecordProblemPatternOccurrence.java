package eu.europeana.metis.sandbox.entity.problempatterns;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity class for record problem pattern occurrence.
 */
@Entity
@Table(schema = "problem_patterns", name = "record_problem_pattern_occurrence")
public class RecordProblemPatternOccurrence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "record_problem_pattern_occurrence_id", nullable = false)
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "record_problem_pattern_id", nullable = false)
  private RecordProblemPattern recordProblemPattern;

  @Column(name = "message_report", nullable = false)
  private String messageReport;

  public String getMessageReport() {
    return messageReport;
  }

  public void setMessageReport(String messageReport) {
    this.messageReport = messageReport;
  }

  public RecordProblemPattern getRecordProblemPattern() {
    return recordProblemPattern;
  }

  public void setRecordProblemPattern(RecordProblemPattern recordProblemPattern) {
    this.recordProblemPattern = recordProblemPattern;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
}
