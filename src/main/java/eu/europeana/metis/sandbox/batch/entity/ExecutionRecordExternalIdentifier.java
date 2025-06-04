package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "engine_record")
public class ExecutionRecordExternalIdentifier implements HasExecutionRecordIdAccess<ExecutionRecordExternalIdentifierKey> {

    @EmbeddedId
    private ExecutionRecordExternalIdentifierKey identifier;
    private boolean isDeleted;
}
