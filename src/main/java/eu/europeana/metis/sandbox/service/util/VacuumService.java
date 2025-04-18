package eu.europeana.metis.sandbox.service.util;

import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * The type Vacuum service.
 */
@Service
public class VacuumService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final JdbcTemplate jdbcTemplate;

  /**
   * Instantiates a new Vacuum service.
   *
   * @param jdbcTemplate the jdbc template
   */
  public VacuumService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Vacuum the listed tables.
   */
  public void vacuum() {
    List<String> tables = List.of(
        "problem_patterns.record_problem_pattern",
        "problem_patterns.record_problem_pattern_occurrence",
        "problem_patterns.record_title",
        "problem_patterns.dataset_problem_pattern",
        "problem_patterns.execution_point",
        "public.record_log",
        "public.record_error_log",
        "public.record",
        "public.harvesting_parameter",
        "public.record_debias_detail",
        "public.record_debias_main",
        "public.dataset_debias_detect",
        "public.dataset_log",
        "public.dataset",
        "integration.int_lock",
        "rate_limit.buckets"
    );
    tables.forEach(table -> {
      LOGGER.info("Performing vacuum of table {}", table);
      jdbcTemplate.execute(String.format("VACUUM %s", table));
      LOGGER.info("Vacuum of table {} completed", table);
    });
  }
}
