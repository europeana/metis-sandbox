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
  public void deleteRedundantRecordTitles(int executionPointId) {
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
  private PreparedStatementCreator getDeleteRedundantPreparedStatementCreator(int executionPointId) {
    return connection -> {
      PreparedStatement deleteRedundantStatement = connection.prepareStatement(
          "DELETE "
              + "FROM problem_patterns.record_title "
              + "WHERE (execution_point_id, record_id, title) IN ("
              + "    /*Inner join the table with the non duplicates to get all columns*/"
              + "    (SELECT x.* "
              + "     FROM problem_patterns.record_title AS x INNER JOIN"
              + "         /*Select all the relevant values that are non duplicates*/"
              + "         (SELECT COUNT(record_id) AS duplicate_count, title"
              + "          FROM problem_patterns.record_title"
              + "          WHERE execution_point_id = ?"
              + "          GROUP BY title"
              + "          HAVING COUNT(record_id) = 1)"
              + "     AS y ON (x.title = y.title) "
              + "     WHERE execution_point_id = ?"
              + "    )"
              + ")"
      );
      deleteRedundantStatement.setInt(1, executionPointId);
      deleteRedundantStatement.setInt(2, executionPointId);
      return deleteRedundantStatement;
    };
  }
}
