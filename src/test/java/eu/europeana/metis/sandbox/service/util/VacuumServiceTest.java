package eu.europeana.metis.sandbox.service.util;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class VacuumServiceTest {

  @Mock
  JdbcTemplate jdbcTemplate;

  @InjectMocks
  VacuumService vacuumService;

  @Test
  void vacuum() {
    doNothing().when(jdbcTemplate).execute(anyString());

    vacuumService.vacuum();

    verify(jdbcTemplate,times(16)).execute(anyString());
  }
}
