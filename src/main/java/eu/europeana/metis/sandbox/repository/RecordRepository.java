package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<RecordEntity, Long> {

}
