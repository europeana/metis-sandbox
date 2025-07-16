package eu.europeana.metis.sandbox.controller;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.config.SecurityConfig;
import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
import eu.europeana.metis.sandbox.controller.advice.ControllerErrorHandler;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.FileHarvestingDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.TierStatistics;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfo;

import eu.europeana.metis.sandbox.service.dataset.DatasetLogService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.security.test.JwtUtils;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(UserDatasetController.class)
@ContextConfiguration(classes = {WebMvcConfig.class, UserDatasetController.class, SecurityConfig.class, ControllerErrorHandler.class})
class UserDatasetControllerTest {

  @MockBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockBean
  private DatasetService datasetService;

  @MockBean
  private DatasetLogService datasetLogService;

  @MockBean
  private DatasetReportService datasetReportService;

  @MockBean
  JwtDecoder jwtDecoder;

  @Mock
  private CompletableFuture<Void> asyncResult;

  private static MockMvc mvc;
  private final JwtUtils jwtUtils;

  public UserDatasetControllerTest() {
    jwtUtils = new JwtUtils(List.of());
  }

  @BeforeAll
  static void setup(WebApplicationContext context) {
    mvc = MockMvcBuilders.webAppContextSetup(context)
                         .apply(SecurityMockMvcConfigurers.springSecurity())
                         .defaultRequest(get("/"))
                         .build();
  }

  @BeforeEach
  public void setup() {
    reset(jwtDecoder);
  }

  private DatasetInfoDto getFakeDatasetInfoDto(String userId, String id) {
    return new DatasetInfoDto.Builder()
      .datasetId(id)
      .createdById(userId)
      .harvestingParametricDto(new FileHarvestingDto("fileName", "fileType"))
      .build();
  }

  private ProgressInfoDto getFakeProgressInfo() {
    var createProgress = new ProgressByStepDto(Step.HARVEST_FILE, 10, 0, 0, List.of());
    var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 7, 3, 0, Collections.emptyList());
    var tiersZeroInfo = new TiersZeroInfo(new TierStatistics(0, Collections.emptyList()),
        new TierStatistics(0, Collections.emptyList()));
    return new ProgressInfoDto("https://metis-sandbox",
        10L, 10L, List.of(createProgress, externalProgress), false, "", emptyList(),
        tiersZeroInfo);
  }

  private void prepateServiceMocks(String userId) {
    List<DatasetInfoDto> datasetInfoDtos = new ArrayList<DatasetInfoDto>();

    datasetInfoDtos.add(getFakeDatasetInfoDto(userId, "1"));
    datasetInfoDtos.add(getFakeDatasetInfoDto(userId, "2"));
    datasetInfoDtos.add(getFakeDatasetInfoDto(userId, "3"));

    when(datasetService.getDatasetsCreatedById(userId)).thenReturn(datasetInfoDtos);

    ProgressInfoDto report = getFakeProgressInfo();

    when(datasetReportService.getReport("1")).thenReturn(report);
    when(datasetReportService.getReport("2")).thenReturn(report);
    when(datasetReportService.getReport("3")).thenReturn(report);
  }

  @Test
  void getUserDatasets_expectFailure() throws Exception {

    String userId = "user";
    prepateServiceMocks(userId);

    mvc.perform(
        get("/user-datasets")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void getUserDatasets_expectSuccess() throws Exception {

    String userId = "user";
    prepateServiceMocks(userId);

    mvc.perform(
        get("/user-datasets")
        .with(SecurityMockMvcRequestPostProcessors.jwt()
          .jwt(jwt -> {
               jwt.claim("sub", userId);
               jwt.claim("scope", "read write");
           })
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasSize(3)));
  }
}
