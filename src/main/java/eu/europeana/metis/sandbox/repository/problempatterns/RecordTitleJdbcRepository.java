package eu.europeana.metis.sandbox.repository.problempatterns;

import java.sql.PreparedStatement;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

/**
 * Jdbc repository for {@link eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle}.
 */
@Repository
public class RecordTitleJdbcRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordTitleJdbcRepository.class);
  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor with required parameters.
   *
   * @param jdbcTemplate the jdbc template
   */
  public RecordTitleJdbcRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Removes non duplicate titles for an execution point id.
   *
   * @param executionPointId the execution point id
   */
  public void deleteRedundantRecordTitles(Integer executionPointId) {
    LOGGER.debug("Request removal of redundant titles for executionPointId: {}", executionPointId);
    final int removedItems = jdbcTemplate.update(getDeleteRedundantPreparedStatementCreator(executionPointId));
    LOGGER.debug("Removed {} items", removedItems);
  }

  /**
   * Creates a prepared statement creator to be used for jdbc query submission.
   * <p>The method handles an sql query that would remove redundant titles(all non-duplicate titles) from the database.</p>
   *
   * @param executionPointId the execution point id
   * @return the prepared statement creator
   */
  @NotNull
  private PreparedStatementCreator getDeleteRedundantPreparedStatementCreator(Integer executionPointId) {
    return connection -> {
      PreparedStatement deleteRedundantStatement = connection.prepareStatement(
          "delete "
              + "from problem_patterns.record_title "
              + "where (execution_point_id, record_id, title) IN ("
              + "    /*Inner join the table with the non duplicates to get all columns*/"
              + "    (select x.* "
              + "     from problem_patterns.record_title as x inner join"
              + "         /*Select all the relevant values that are non duplicates*/"
              + "         (select count(record_id) as duplicate_count, title"
              + "          from problem_patterns.record_title"
              + "          where execution_point_id = ?"
              + "          group by title"
              + "          having count(record_id) = 1)"
              + "     as y on (x.title = y.title) "
              + "     where execution_point_id = ?"
              + "    )"
              + ")"
      );
      deleteRedundantStatement.setInt(1, executionPointId);
      deleteRedundantStatement.setInt(2, executionPointId);
      return deleteRedundantStatement;
    };
  }
}
