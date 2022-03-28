package eu.europeana.metis.sandbox.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Status;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ErrorInfoDto}
 */
class ErrorInfoDtoTest {

  @Test
  void getRecordIds() {
    ErrorInfoDto errorInfoDto = getTestErrorInfoDto();

    assertEquals(List.of("1", "2", "3", "4", "5"), errorInfoDto.getRecordIds());
  }

  @Test
  void getType() {
    ErrorInfoDto errorInfoDto = getTestErrorInfoDto();

    assertEquals(Status.WARN, errorInfoDto.getType());
  }

  @Test
  void getErrorMessage() {
    ErrorInfoDto errorInfoDto = getTestErrorInfoDto();

    assertEquals("warn message", errorInfoDto.getErrorMessage());
  }

  @Test
  void testEquals() {
    ErrorInfoDto errorInfoDto1 = getTestErrorInfoDto();
    ErrorInfoDto errorInfoDto2 = getTestErrorInfoDto();

    assertTrue(errorInfoDto1.equals(errorInfoDto2) && errorInfoDto2.equals(errorInfoDto1));
    assertFalse(errorInfoDto1.equals(null) && errorInfoDto2.equals(null));
  }

  @Test
  void testHashCode() {
    ErrorInfoDto errorInfoDto1 = getTestErrorInfoDto();
    ErrorInfoDto errorInfoDto2 = getTestErrorInfoDto();

    assertEquals(errorInfoDto1.hashCode(), errorInfoDto2.hashCode());
  }

  @NotNull
  private static ErrorInfoDto getTestErrorInfoDto() {
    return new ErrorInfoDto("warn message",
        Status.WARN,
        List.of("1", "2", "3", "4", "5"));
  }
}