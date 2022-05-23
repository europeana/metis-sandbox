package eu.europeana.metis.sandbox.repository.problempatterns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import eu.europeana.metis.test.utils.PostgresContainerInitializerIT;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
//Only load what we are going to use. This is simpler than specifying all the autoconfigured classes
@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class RecordTitleJdbcRepositoryIT extends PostgresContainerInitializerIT {

  private static final String SQL_EXECUTION_POINT_COUNT = "SELECT count(*) from problem_patterns.execution_point";
  public static final String SQL_SELECT_RECORD_TITLES = "SELECT * FROM problem_patterns.record_title";
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private RecordTitleJdbcRepository recordTitleJdbcRepository;

  @Test
  void deleteRedundantRecordTitlesTest() {
    insertValues();

    Integer executionPointCount = jdbcTemplate.queryForObject(SQL_EXECUTION_POINT_COUNT, Integer.class);
    List<RecordTitle> recordTitles = jdbcTemplate.query(SQL_SELECT_RECORD_TITLES, new RecordTitleRowMapper());

    assertEquals(1, executionPointCount);
    assertEquals(5, recordTitles.size());
    recordTitleJdbcRepository.deleteRedundantRecordTitles(1);
    executionPointCount = jdbcTemplate.queryForObject(SQL_EXECUTION_POINT_COUNT, Integer.class);
    recordTitles = jdbcTemplate.query(SQL_SELECT_RECORD_TITLES, new RecordTitleRowMapper());
    assertEquals(2, recordTitles.size());
    assertEquals(1, executionPointCount);
    assertTrue(
        recordTitles.stream().allMatch(recordTitle -> recordTitle.getRecordTitleCompositeKey().getTitle().equals("titleA")));

    cleanUp();

  }

  private void insertValues() {
    jdbcTemplate.update(
        "INSERT INTO problem_patterns.execution_point (dataset_id, execution_step, execution_timestamp) VALUES (1, 'VALIDATION_EXTERNAL', '2022-03-22 10:10:10.100 +02:00');"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId1', 'titleA');"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId1', 'titleS');\n"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId1', 'Some ValueC');\n"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId2', 'titleA');\n"
            + "INSERT INTO problem_patterns.record_title (execution_point_id, record_id, title) VALUES (1, 'recordId2', 'titleB');");
  }

  public void cleanUp() {
    jdbcTemplate.execute("DELETE FROM problem_patterns.record_title");
    jdbcTemplate.execute("DELETE FROM problem_patterns.execution_point");
    assertEquals(0, jdbcTemplate.queryForObject(SQL_EXECUTION_POINT_COUNT, Integer.class));
    assertEquals(0, jdbcTemplate.query(SQL_SELECT_RECORD_TITLES, new RecordTitleRowMapper()).size());
  }

  static class RecordTitleRowMapper implements RowMapper<RecordTitle> {

    @Override
    public RecordTitle mapRow(ResultSet rs, int rowNumber) throws SQLException {
      return new RecordTitle(
          new RecordTitleCompositeKey(rs.getInt("execution_point_id"), rs.getString("record_id"), rs.getString("title")), null);
    }
  }
}