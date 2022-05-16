package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordTitleRepository extends JpaRepository<RecordTitle, RecordTitleCompositeKey> {

}
