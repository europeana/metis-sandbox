package eu.europeana.metis.sandbox.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Status;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ErrorInfoDTO}
 */
class ErrorInfoDTOTest {

  @Test
  void getRecordIds() {
    ErrorInfoDTO errorInfoDto = getTestErrorInfoDto();

    assertEquals(List.of("1", "2", "3", "4", "5"), errorInfoDto.getRecordIds());
  }

  @Test
  void getType() {
    ErrorInfoDTO errorInfoDto = getTestErrorInfoDto();

    assertEquals(Status.WARN, errorInfoDto.getType());
  }

  @Test
  void getErrorMessage() {
    ErrorInfoDTO errorInfoDto = getTestErrorInfoDto();

    assertEquals("warn message", errorInfoDto.getErrorMessage());
  }

  @Test
  void testEquals() {
    ErrorInfoDTO errorInfoDTO1 = getTestErrorInfoDto();
    ErrorInfoDTO errorInfoDTO2 = getTestErrorInfoDto();

    assertTrue(errorInfoDTO1.equals(errorInfoDTO2) && errorInfoDTO2.equals(errorInfoDTO1));
    assertFalse(errorInfoDTO1.equals(null) && errorInfoDTO2.equals(null));
  }

  @Test
  void testHashCode() {
    ErrorInfoDTO errorInfoDTO1 = getTestErrorInfoDto();
    ErrorInfoDTO errorInfoDTO2 = getTestErrorInfoDto();

    assertEquals(errorInfoDTO1.hashCode(), errorInfoDTO2.hashCode());
  }

  @NotNull
  private static ErrorInfoDTO getTestErrorInfoDto() {
    return new ErrorInfoDTO("warn message",
        Status.WARN,
        List.of("1", "2", "3", "4", "5"));
  }
}