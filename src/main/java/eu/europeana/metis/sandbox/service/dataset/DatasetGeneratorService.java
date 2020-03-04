package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import java.util.List;

public interface DatasetGeneratorService {

  /**
   * Creates a {@link Dataset} instance
   *
   * @param name     must not be null
   * @param country  must not be null
   * @param language must not be null
   * @param records  must not be null or empty
   * @return {@link Dataset}
   * @throws NullPointerException     if any parameter is null
   * @throws IllegalArgumentException if records list is empty
   */
  Dataset generate(String name, Country country, Language language,
      List<String> records);
}
