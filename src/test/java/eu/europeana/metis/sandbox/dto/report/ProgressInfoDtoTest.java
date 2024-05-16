package eu.europeana.metis.sandbox.dto.report;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto.Status;

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

    private static Stream<Arguments> provideStatus() {
        return Stream.of(
                Arguments.of(getTestHarvestingIdsInfoDto(), Status.HARVESTING_IDENTIFIERS),
                Arguments.of(getTestProgressInfoDto(), Status.IN_PROGRESS),
                Arguments.of(getTestCompletedInfoDto(), Status.COMPLETED)
        );
    }

    private static Stream<Arguments> provideProcessed() {
        return Stream.of(
                Arguments.of(getTestHarvestingIdsInfoDto(), 0L),
                Arguments.of(getTestProgressInfoDto(), 2L),
                Arguments.of(getTestCompletedInfoDto(), 5L)
        );
    }

    private static Stream<Arguments> providePublish() {
        return Stream.of(
                Arguments.of(getTestHarvestingIdsInfoDto(), false, ""),
                Arguments.of(getTestErrorTypeInfoDto(), true, "http://metis-sandbox"),
                Arguments.of(getTestProgressInfoDto(), true, "http://metis-sandbox"),
                Arguments.of(getTestCompletedInfoDto(), true, "http://metis-sandbox"),
                Arguments.of(getTestFailedInfoDtoNotPublished(), false, "")
        );
    }

    @NotNull
    private static ProgressInfoDto getTestProgressInfoDto() {
        return new ProgressInfoDto("http://metis-sandbox",
                5L,
                2L,
                getProgressByStepDtoList(2, 1),
                false, "", emptyList(), null);
    }

    @NotNull
    private static List<ProgressByStepDto> getProgressByStepDtoList(int mediaProcessed,
                                                                    int published) {
        return List.of(new ProgressByStepDto(Step.HARVEST_FILE, 5, 0, 0, List.of()),
                new ProgressByStepDto(Step.TRANSFORM_TO_EDM_EXTERNAL, 5, 0, 0, List.of()),
                new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 5, 0, 0, List.of()),
                new ProgressByStepDto(Step.TRANSFORM, 5, 0, 0, List.of()),
                new ProgressByStepDto(Step.VALIDATE_INTERNAL, 5, 0, 0, List.of()),
                new ProgressByStepDto(Step.NORMALIZE, 5, 0, 0, List.of()),
                new ProgressByStepDto(Step.ENRICH, 5, 0, 0, List.of()),
                new ProgressByStepDto(Step.MEDIA_PROCESS, mediaProcessed, 0, 0, List.of()),
                new ProgressByStepDto(Step.PUBLISH, published, 0, 0, List.of()));
    }

    @NotNull
    private static ProgressInfoDto getTestHarvestingIdsInfoDto() {
        return new ProgressInfoDto("http://metis-sandbox",
                null,
                0L,
                List.of(new ProgressByStepDto(Step.HARVEST_FILE, 0, 0, 0, List.of())),
                false, "", emptyList(), null);
    }

    @NotNull
    private static ProgressInfoDto getTestFailedInfoDtoNotPublished() {
        return new ProgressInfoDto("http://metis-sandbox",
                5L,
                5L,
                getProgressByStepDtoList(5, 0),
                false, "Fail to publish", emptyList(),
                getTiersZeroInfo());
    }

    @NotNull
    private static ProgressInfoDto getTestCompletedInfoDto() {
        return new ProgressInfoDto("http://metis-sandbox",
                5L,
                5L,
                getProgressByStepDtoList(5, 5),
                false,
                "", emptyList(),
                getTiersZeroInfo());
    }

    @NotNull
    private static ProgressInfoDto getTestErrorTypeInfoDto() {
        return new ProgressInfoDto("http://metis-sandbox",
                5L,
                5L,
                getProgressByStepDtoList(5, 5),
                false,
                "Error",
                emptyList(),
                null);
    }

    @NotNull
    private static TiersZeroInfo getTiersZeroInfo() {
        TierStatistics contentTier = new TierStatistics(2, List.of("europeanaId1", "europeanaId2"));
        TierStatistics metadataTier = new TierStatistics(4,
                List.of("europeanaId1", "europeanaId2", "europeanaId3", "europeanaId4"));

        return new TiersZeroInfo(contentTier, metadataTier);
    }

    @Test
    void getPortalPublishUrl() {
        progressInfoDto = getTestProgressInfoDto();
        assertEquals("http://metis-sandbox", progressInfoDto.getPortalPublishUrl());
    }

    @ParameterizedTest
    @MethodSource("provideStatus")
    void getStatus(ProgressInfoDto progressInfoDto, Status expectedStatus) {
        assertEquals(expectedStatus, progressInfoDto.getStatus());
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
    void getRecordLimitExceeded() {
        progressInfoDto = getTestProgressInfoDto();
        assertFalse(progressInfoDto.getRecordLimitExceeded());

    }

    @ParameterizedTest
    @MethodSource("providePublish")
    void getRecordsPublishedSuccessfully(ProgressInfoDto progressInfoDto,
                                         boolean expectedPublishedSuccess,
                                         String expectedPublishPortal) {
        assertEquals(expectedPublishPortal, progressInfoDto.getPortalPublishUrl());
        assertEquals(expectedPublishedSuccess, progressInfoDto.isRecordsPublishedSuccessfully());
    }

    @Test
    void getErrorType() {
        progressInfoDto = getTestErrorTypeInfoDto();
        assertEquals("Error", progressInfoDto.getErrorType());
        assertEquals(Status.FAILED, progressInfoDto.getStatus());
        assertEquals("http://metis-sandbox", progressInfoDto.getPortalPublishUrl());
    }

    @Test
    void getTiersZeroInfoTest() {
        progressInfoDto = getTestCompletedInfoDto();
        assertEquals(2, progressInfoDto.getTiersZeroInfo().getContentTier().getTotalNumberOfRecords());
        assertEquals(progressInfoDto.getTiersZeroInfo().getContentTier().getListRecordIds(),
                List.of("europeanaId1", "europeanaId2"));
        assertEquals(4, progressInfoDto.getTiersZeroInfo().getMetadataTier().getTotalNumberOfRecords());
        assertEquals(progressInfoDto.getTiersZeroInfo().getMetadataTier().getListRecordIds(),
                List.of("europeanaId1", "europeanaId2", "europeanaId3", "europeanaId4"));
    }

}
