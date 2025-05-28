package eu.europeana.metis.sandbox.repository.problempatterns;

import java.lang.invoke.MethodHandles;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
          "DELETE FROM problem_patterns.record_title rt " +
              "USING (" +
              // Subquery selecting redundant (non-duplicate) titles for given execution point
              "  SELECT x.execution_point_id, x.record_id, x.title " +
              "  FROM problem_patterns.record_title x " +
              "  INNER JOIN (" +
              "    SELECT MIN(record_id) AS record_id, UPPER(title) AS uppercase_title " +
              "    FROM problem_patterns.record_title " +
              "    WHERE execution_point_id = ? " +
              "    GROUP BY UPPER(title) " +
              "    HAVING COUNT(*) = 1" +
              "  ) y ON UPPER(x.title) = y.uppercase_title AND x.record_id = y.record_id " +
              "  WHERE x.execution_point_id = ?" +
              ") sub " +
              "WHERE rt.execution_point_id = sub.execution_point_id " +
              "AND rt.record_id = sub.record_id " +
              "AND rt.title = sub.title"
      );
      deleteRedundantStatement.setInt(1, executionPointId);
      deleteRedundantStatement.setInt(2, executionPointId);
      return deleteRedundantStatement;
    };
  }
}
