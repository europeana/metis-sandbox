package eu.europeana.metis.sandbox.repository;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

/**
 * Jdbc repository for {@link RecordRepository}
 */
@Repository
public class RecordJdbcRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor with required parameters.
   *
   * @param jdbcTemplate the jdbc template
   */
  public RecordJdbcRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Update record with new values for europeana id and provider id
   *
   * @param recordId the id of the record to update
   * @param europeanaId the europeana id value to update with
   * @param providerId the provider id value to update with
   * @param datasetId the dataset
   * @return amount of records updated
   */
  public Integer updateRecord(long recordId, String europeanaId, String providerId, String datasetId) {
    final Integer result = jdbcTemplate.execute(
        updateRecordIfNoConflict(recordId, europeanaId, providerId, datasetId), preparedStatement ->
        {
          try {
            return preparedStatement.executeUpdate();
          } catch (PSQLException e) {
            if (e.getMessage().contains("ERROR: duplicate key value violates unique constraint")) {
              return 0;
            } else {
              return -1;
            }
          }
        });
    LOGGER.debug("Counter after executing update: {}", result);
    return result;
  }

  @NotNull
  private PreparedStatementCreator updateRecordIfNoConflict(long recordId, String europeanaId, String providerId,
      String datasetId) {
    return connection -> {
      PreparedStatement statement = connection.prepareStatement(
          "UPDATE record rec "
              + "SET (europeana_id, provider_id) = (?, ?) "
              + "WHERE rec.id = ? AND rec.dataset_id =? "
              + "AND NOT EXISTS ("
              + "SELECT * FROM record other WHERE other.europeana_id =? AND other.provider_id = ? "
              + "AND rec.dataset_id = other.dataset_id)");
      statement.setString(1, europeanaId);
      statement.setString(2, providerId);
      statement.setLong(3, recordId);
      statement.setString(4, datasetId);
      statement.setString(5, europeanaId);
      statement.setString(6, providerId);
      return statement;
    };
  }
}
