package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordLogRepository extends JpaRepository<RecordLogEntity, Long> {

}
