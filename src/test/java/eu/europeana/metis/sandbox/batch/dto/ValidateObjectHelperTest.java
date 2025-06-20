package eu.europeana.metis.sandbox.batch.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.ValidateObjectHelper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.Test;

class ValidateObjectHelperTest {

  @Value
  @Builder
  static class TestDto {

    String id;
    @NotBlank
    String name;
  }

  @Test
  void testValidDto_buildValidated_shouldReturnDto() {
    TestDto dto = ValidateObjectHelper.buildValidated(
        TestDto.TestDtoBuilder::new,
        TestDto.TestDtoBuilder::build,
        builder -> builder.name("Name")
    );

    assertNotNull(dto);
    assertEquals("Name", dto.getName());
  }

  @Test
  void testInvalidDto_buildValidated_shouldThrowException() {
    ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () ->
        ValidateObjectHelper.buildValidated(
            TestDto.TestDtoBuilder::new,
            TestDto.TestDtoBuilder::build,
            builder -> builder.name("")
        )
    );

    assertTrue(exception.getMessage().contains("name"));
  }

  @Test
  void testValidDtoWithPreBuilderSetup_shouldReturnDto() {
    TestDto dto = ValidateObjectHelper.buildValidated(
        TestDto.TestDtoBuilder::new,
        TestDto.TestDtoBuilder::build,
        preBuilder -> preBuilder.id("id"), // nothing
        builder -> builder.name("Name")
    );

    assertNotNull(dto);
    assertEquals("id", dto.getId());
    assertEquals("Name", dto.getName());
  }

  @Test
  void testPreBuilderSetupRunsBeforeBuilderSetup() {
    List<Integer> steps = new ArrayList<>();

    TestDto dto = ValidateObjectHelper.buildValidated(
        TestDto.TestDtoBuilder::new,
        TestDto.TestDtoBuilder::build,
        preBuilder -> steps.add(1),
        builder -> {
          steps.add(2);
          builder.name("Name");
        }
    );

    assertNotNull(dto);
    assertEquals(List.of(1, 2), steps);
  }
}
