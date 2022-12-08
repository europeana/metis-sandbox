package eu.europeana.metis.sandbox.repository;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;
import org.postgresql.util.PSQLException;

import java.sql.PreparedStatement;

/**
 * Jdbc repository for {@link RecordRepository}
 */
@Repository
public class RecordJdbcRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordJdbcRepository.class);
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor with required parameters.
     *
     * @param jdbcTemplate the jdbc template
     */
    public RecordJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer upsertRecord(long recordId, String europeanaId, String providerId, String datasetId) {
        final Integer result = jdbcTemplate.execute(
                getUpsertIdIfExists(recordId, europeanaId, providerId, datasetId), preparedStatement ->
                {
                    try{
                       return preparedStatement.executeUpdate();
                    } catch (PSQLException e){
                        if(e.getMessage().contains("ERROR: duplicate key value violates unique constraint")) {
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
    private PreparedStatementCreator getUpsertIdIfExists(long recordId, String europeanaId, String providerId, String datasetId) {
        return connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE record rec "
                            + "SET (europeana_id, provider_id) = (?, ?) "
                            + "WHERE rec.id = ? AND rec.dataset_id =? "
                            + "AND NOT EXISTS ("
                            + "SELECT * FROM record other WHERE other.provider_id = ? AND other.dataset_id = ?)");
            statement.setString(1, europeanaId);
            statement.setString(2, providerId);
            statement.setLong(3, recordId);
            statement.setString(4, datasetId);
            statement.setString(5, providerId);
            statement.setString(6, datasetId);
            return statement;
        };
    }
}
