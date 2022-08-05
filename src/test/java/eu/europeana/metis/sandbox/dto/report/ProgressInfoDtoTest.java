package eu.europeana.metis.sandbox.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit test for {@link ProgressInfoDto}
 */
class ProgressInfoDtoTest {

  private ProgressInfoDto progressInfoDto;

  @Test
  void getPortalPublishUrl() {
    progressInfoDto = getTestProgressInfoDto();
    assertEquals("http://metis-sandbox", progressInfoDto.getPortalPublishUrl());
  }

  private static Stream<Arguments> provideStatus() {
    return Stream.of(
        Arguments.of(getTestHarvestingIdsInfoDto(), Status.HARVESTING_IDENTIFIERS),
        Arguments.of(getTestProgressInfoDto(), Status.IN_PROGRESS),
        Arguments.of(getTestCompletedInfoDto(), Status.COMPLETED)
    );
  }

  @ParameterizedTest
  @MethodSource("provideStatus")
  void getStatus(ProgressInfoDto progressInfoDto, Status expectedStatus) {
    assertEquals(expectedStatus, progressInfoDto.getStatus());
  }

  private static Stream<Arguments> provideProcessed() {
    return Stream.of(
        Arguments.of(getTestHarvestingIdsInfoDto(), 0L),
        Arguments.of(getTestProgressInfoDto(), 2L),
        Arguments.of(getTestCompletedInfoDto(), 5L)
    );
  }

  @ParameterizedTest
  @MethodSource("provideProcessed")
  void getProcessedRecords(ProgressInfoDto progressInfoDto, Long expectedProcessed) {
    assertEquals(expectedProcessed, progressInfoDto.getProcessedRecords());
  }

  @Test
  void getProgressByStep() {
    progressInfoDto = getTestProgressInfoDto();
    final List<ProgressByStepDto> expectedProgressByStepDtoList = getProgressByStepDtoList(2, 1);
    assertTrue(expectedProgressByStepDtoList.containsAll(progressInfoDto.getProgressByStep()));
  }

  @Test
  void getDatasetInfoDto() {
    progressInfoDto = getTestProgressInfoDto();

    assertEquals("datasetId", progressInfoDto.getDatasetInfoDto().getDatasetId());
    assertEquals("datasetName", progressInfoDto.getDatasetInfoDto().getDatasetName());
    assertEquals(LocalDateTime.parse("2022-03-14T22:50:22"), progressInfoDto.getDatasetInfoDto().getCreationDate());
    assertEquals(Country.CROATIA, progressInfoDto.getDatasetInfoDto().getCountry());
    assertEquals(Language.HR, progressInfoDto.getDatasetInfoDto().getLanguage());
    assertFalse(progressInfoDto.getDatasetInfoDto().isRecordLimitExceeded());
    assertFalse(progressInfoDto.getDatasetInfoDto().isTransformedToEdmExternal());
  }

  @NotNull
  private static ProgressInfoDto getTestProgressInfoDto() {
    return new ProgressInfoDto("http://metis-sandbox",
        5L,
        2L,
        getProgressByStepDtoList(2, 1),
        new DatasetInfoDto("datasetId",
            "datasetName",
            LocalDateTime.parse("2022-03-14T22:50:22"),
            Language.HR,
            Country.CROATIA,
            false,
            false), "", null);
  }

  @NotNull
  private static List<ProgressByStepDto> getProgressByStepDtoList(int success, int success1) {
    return List.of(new ProgressByStepDto(Step.HARVEST_ZIP, 5, 0, 0, List.of()),
        new ProgressByStepDto(Step.TRANSFORM_TO_EDM_EXTERNAL, 5, 0, 0, List.of()),
        new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 5, 0, 0, List.of()),
        new ProgressByStepDto(Step.TRANSFORM, 5, 0, 0, List.of()),
        new ProgressByStepDto(Step.VALIDATE_INTERNAL, 5, 0, 0, List.of()),
        new ProgressByStepDto(Step.NORMALIZE, 5, 0, 0, List.of()),
        new ProgressByStepDto(Step.ENRICH, 5, 0, 0, List.of()),
        new ProgressByStepDto(Step.MEDIA_PROCESS, success, 0, 0, List.of()),
        new ProgressByStepDto(Step.PUBLISH, success1, 0, 0, List.of()));
  }

  @NotNull
  private static ProgressInfoDto getTestHarvestingIdsInfoDto() {
    return new ProgressInfoDto("http://metis-sandbox",
        null,
        0L,
        List.of(new ProgressByStepDto(Step.HARVEST_ZIP, 0, 0, 0, List.of())),
        new DatasetInfoDto("datasetId",
            "datasetName",
            LocalDateTime.parse("2022-03-14T22:50:22"),
            Language.HR,
            Country.CROATIA,
            false,
            false), "", null);
  }

  @NotNull
  private static ProgressInfoDto getTestCompletedInfoDto() {
    return new ProgressInfoDto("http://metis-sandbox",
        5L,
        5L,
        getProgressByStepDtoList(5, 5),
        new DatasetInfoDto("datasetId",
            "datasetName",
            LocalDateTime.parse("2022-03-14T22:50:22"),
            Language.HR,
            Country.CROATIA,
            false,
            false), "", null);
  }

  @NotNull
  private static ProgressInfoDto getTestErroTypeInfoDto() {
    return new ProgressInfoDto("http://metis-sandbox",
            5L,
            5L,
            getProgressByStepDtoList(5, 5),
            new DatasetInfoDto("datasetId",
                    "datasetName",
                    LocalDateTime.parse("2022-03-14T22:50:22"),
                    Language.HR,
                    Country.CROATIA,
                    false,
                    false), "Error", null);
  }

  @Test
  void getErrorType() {
    progressInfoDto = getTestErroTypeInfoDto();
    assertEquals( "Error", progressInfoDto.getErrorType());
    assertEquals( "", progressInfoDto.getPortalPublishUrl());
  }
}
