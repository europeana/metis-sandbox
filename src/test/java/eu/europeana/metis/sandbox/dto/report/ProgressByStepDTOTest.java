package eu.europeana.metis.sandbox.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.Status;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ProgressByStepDTO}
 */
class ProgressByStepDTOTest {

  @Test
  void getStep() {
    ProgressByStepDTO progressByStepDto = getTestProgressByStepDto();

    assertEquals(FullBatchJobType.TRANSFORM_INTERNAL, progressByStepDto.step());
  }

  @Test
  void getTotal() {
    ProgressByStepDTO progressByStepDto = getTestProgressByStepDto();

    assertEquals(19L, progressByStepDto.total());
  }

  @Test
  void getSuccess() {
    ProgressByStepDTO progressByStepDto = getTestProgressByStepDto();

    assertEquals(12L, progressByStepDto.success());
  }

  @Test
  void getFail() {
    ProgressByStepDTO progressByStepDto = getTestProgressByStepDto();

    assertEquals(5L, progressByStepDto.fail());
  }

  @Test
  void getWarn() {
    ProgressByStepDTO progressByStepDto = getTestProgressByStepDto();

    assertEquals(2L, progressByStepDto.warn());
  }

  @Test
  void getErrors() {
    ProgressByStepDTO progressByStepDto = getTestProgressByStepDto();

    assertEquals(List.of(new ErrorInfoDTO("fail message", Status.FAIL, List.of("1", "2", "3", "4", "5")),
        new ErrorInfoDTO("warn message", Status.WARN, List.of("6", "7"))), progressByStepDto.errors());
  }

  @Test
  void testEquals() {
    ProgressByStepDTO progressByStepDTO1 = getTestProgressByStepDto();
    ProgressByStepDTO progressByStepDTO2 = getTestProgressByStepDto();

    assertTrue(progressByStepDTO1.equals(progressByStepDTO2) && progressByStepDTO2.equals(progressByStepDTO1));
    assertFalse( progressByStepDTO1.equals(null) && progressByStepDTO2.equals(null));
  }

  @Test
  void testHashCode() {
    ProgressByStepDTO progressByStepDTO1 = getTestProgressByStepDto();
    ProgressByStepDTO progressByStepDTO2 = getTestProgressByStepDto();

    assertEquals(progressByStepDTO2.hashCode(), progressByStepDTO1.hashCode());
  }

  @NotNull
  private static ProgressByStepDTO getTestProgressByStepDto() {
    return new ProgressByStepDTO(FullBatchJobType.TRANSFORM_INTERNAL,
        17,
        12,
        5,
        2,
        List.of(new ErrorInfoDTO("fail message",
                Status.FAIL, List.of("1", "2", "3", "4", "5")),
            new ErrorInfoDTO("warn message",
                Status.WARN, List.of("6", "7"))));
  }
}