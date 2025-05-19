package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "batch-framework")
public class ExecutionRecordExternalIdentifier implements HasExecutionRecordIdentifier {

    @EmbeddedId
    private ExecutionRecordIdentifier identifier;
    private boolean isDeleted;
}
