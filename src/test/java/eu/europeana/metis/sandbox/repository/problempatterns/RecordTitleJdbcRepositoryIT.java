package eu.europeana.metis.sandbox.repository.problempatterns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.jdbc.JdbcTestUtils.countRowsInTable;
import static org.springframework.test.jdbc.JdbcTestUtils.deleteFromTables;

import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import eu.europeana.metis.sandbox.test.utils.PostgresContainerInitializerIT;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) //Do not allow JdbcTest to replace the Datasource
@ContextConfiguration(classes = RecordTitleJdbcRepository.class)
class RecordTitleJdbcRepositoryIT {

  @DynamicPropertySource
  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    PostgresContainerInitializerIT.dynamicProperties(registry);
    PostgresContainerInitializerIT.runScripts(List.of(
        "database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql"));
  }

  public static final String SELECT_RECORD_TITLES_QUERY = "SELECT * FROM problem_patterns.record_title";
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private RecordTitleJdbcRepository recordTitleJdbcRepository;

  @Test
  void deleteRedundantRecordTitlesTest() {
    insertValues();

    int executionPointCount = countRowsInTable(jdbcTemplate, "problem_patterns.execution_point");
    List<RecordTitle> recordTitles = jdbcTemplate.query(SELECT_RECORD_TITLES_QUERY, new RecordTitleRowMapper());

    assertEquals(1, executionPointCount);
    assertEquals(5, recordTitles.size());
    recordTitleJdbcRepository.deleteRedundantRecordTitles(1);
    executionPointCount = countRowsInTable(jdbcTemplate, "problem_patterns.execution_point");
    recordTitles = jdbcTemplate.query(SELECT_RECORD_TITLES_QUERY, new RecordTitleRowMapper());
    assertEquals(2, recordTitles.size());
    assertEquals(1, executionPointCount);
    assertTrue(
        recordTitles.stream().allMatch(recordTitle -> recordTitle.getRecordTitleCompositeKey().getTitle().equals("titleA")));

    //Cleanup
    deleteFromTables(jdbcTemplate, "problem_patterns.record_title", "problem_patterns.execution_point");
    assertEquals(0, countRowsInTable(jdbcTemplate, "problem_patterns.execution_point"));
    assertEquals(0, jdbcTemplate.query(SELECT_RECORD_TITLES_QUERY, new RecordTitleRowMapper()).size());
  }

  private void insertValues() {
    jdbcTemplate.update(
        "INSERT INTO problem_patterns.execution_point (dataset_id, execution_step, execution_timestamp) VALUES (1, 'VALIDATION_EXTERNAL', '2022-03-22 10:10:10.100 +02:00');"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId1', 'titleA');"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId1', 'titleS');"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId1', 'Some ValueC');"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId2', 'titleA');"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId2', 'titleB');");
  }

  static class RecordTitleRowMapper implements RowMapper<RecordTitle> {

    @Override
    public RecordTitle mapRow(ResultSet rs, int rowNumber) throws SQLException {
      return new RecordTitle(
          new RecordTitleCompositeKey(rs.getInt("execution_point_id"), rs.getString("record_id"), rs.getString("title")), null);
    }
  }
}