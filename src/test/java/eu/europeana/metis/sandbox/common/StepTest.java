package eu.europeana.metis.sandbox.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit test for {@link Step}
 */
class StepTest {

  private static Stream<Arguments> provideExpectedStepsAndValues() {
    return Stream.of(
        Arguments.of(Step.HARVEST_OAI_PMH, "harvest OAI-PMH", 1),
        Arguments.of(Step.HARVEST_ZIP, "harvest zip", 2),
        Arguments.of(Step.TRANSFORM_TO_EDM_EXTERNAL, "transform to EDM external", 3),
        Arguments.of(Step.VALIDATE_EXTERNAL, "validate (edm external)", 4),
        Arguments.of(Step.TRANSFORM, "transform", 5),
        Arguments.of(Step.VALIDATE_INTERNAL, "validate (edm internal)", 6),
        Arguments.of(Step.NORMALIZE, "normalise", 7),
        Arguments.of(Step.ENRICH, "enrich", 8),
        Arguments.of(Step.MEDIA_PROCESS, "process media", 9),
        Arguments.of(Step.PUBLISH, "publish", 10),
        Arguments.of(Step.CLOSE, "close", 11)
    );
  }

  @ParameterizedTest
  @MethodSource("provideExpectedStepsAndValues")
  void valueOf(Step expectedStep, String value, Integer precedence) {
    assertEquals(expectedStep.value(), value);
    assertEquals(expectedStep.precedence(), precedence);
  }
}