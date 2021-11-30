package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransformXsltRepository extends JpaRepository<TransformXsltEntity, Integer> {

  Optional<TransformXsltEntity> findByTransformXslt(String transformXslt);

}
