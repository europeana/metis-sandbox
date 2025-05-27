package eu.europeana.metis.sandbox.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ProgressByStepDto}
 */
class ProgressByStepDtoTest {

  @Test
  void getStep() {
    ProgressByStepDto progressByStepDto = getTestProgressByStepDto();

    assertEquals(Step.TRANSFORM_INTERNAL, progressByStepDto.getStep());
  }

  @Test
  void getTotal() {
    ProgressByStepDto progressByStepDto = getTestProgressByStepDto();

    assertEquals(19L, progressByStepDto.getTotal());
  }

  @Test
  void getSuccess() {
    ProgressByStepDto progressByStepDto = getTestProgressByStepDto();

    assertEquals(12L, progressByStepDto.getSuccess());
  }

  @Test
  void getFail() {
    ProgressByStepDto progressByStepDto = getTestProgressByStepDto();

    assertEquals(5L, progressByStepDto.getFail());
  }

  @Test
  void getWarn() {
    ProgressByStepDto progressByStepDto = getTestProgressByStepDto();

    assertEquals(2L, progressByStepDto.getWarn());
  }

  @Test
  void getErrors() {
    ProgressByStepDto progressByStepDto = getTestProgressByStepDto();

    assertEquals(List.of(new ErrorInfoDto("fail message", Status.FAIL, List.of("1", "2", "3", "4", "5")),
        new ErrorInfoDto("warn message", Status.WARN, List.of("6", "7"))), progressByStepDto.getErrors());
  }

  @Test
  void testEquals() {
    ProgressByStepDto progressByStepDto1 = getTestProgressByStepDto();
    ProgressByStepDto progressByStepDto2 = getTestProgressByStepDto();

    assertTrue(progressByStepDto1.equals(progressByStepDto2) && progressByStepDto2.equals(progressByStepDto1));
    assertFalse( progressByStepDto1.equals(null) && progressByStepDto2.equals(null));
  }

  @Test
  void testHashCode() {
    ProgressByStepDto progressByStepDto1 = getTestProgressByStepDto();
    ProgressByStepDto progressByStepDto2 = getTestProgressByStepDto();

    assertEquals(progressByStepDto2.hashCode(), progressByStepDto1.hashCode());
  }

  @NotNull
  private static ProgressByStepDto getTestProgressByStepDto() {
    return new ProgressByStepDto(Step.TRANSFORM_INTERNAL,
        12,
        5,
        2,
        List.of(new ErrorInfoDto("fail message",
                Status.FAIL, List.of("1", "2", "3", "4", "5")),
            new ErrorInfoDto("warn message",
                Status.WARN, List.of("6", "7"))));
  }
}