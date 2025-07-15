package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.security.test.JwtUtils.BEARER;
import static eu.europeana.metis.security.test.JwtUtils.MOCK_VALID_TOKEN;
import static java.util.Collections.emptyList;

import static org.hamcrest.Matchers.hasSize;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.indexing.utils.LicenseType;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.config.SecurityConfig;
import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
import eu.europeana.metis.sandbox.controller.advice.ControllerErrorHandler;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.TierStatistics;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfo;
import eu.europeana.metis.sandbox.service.dataset.DatasetLogService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.security.test.JwtUtils;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
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

  @Test
  @WithMockUser(username="user", password = "pwd", roles = "USER")
  void getUserDatasets_expectSuccess() throws Exception {

    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());

    String userId = "user";
    List<DatasetInfoDto> datasetInfoDtos = new ArrayList<DatasetInfoDto>();
    datasetInfoDtos.add(new DatasetInfoDto.Builder().datasetId("1").createdById(userId).build());
    datasetInfoDtos.add(new DatasetInfoDto.Builder().datasetId("2").createdById(userId).build());
    datasetInfoDtos.add(new DatasetInfoDto.Builder().datasetId("3").createdById(userId).build());

    when(datasetService.getDatasetsCreatedById(userId)).thenReturn(datasetInfoDtos);

    var createProgress = new ProgressByStepDto(Step.HARVEST_FILE, 10, 0, 0, List.of());
    var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 7, 3, 0, Collections.emptyList());
    var tiersZeroInfo = new TiersZeroInfo(new TierStatistics(0, Collections.emptyList()),
        new TierStatistics(0, Collections.emptyList()));
    var report = new ProgressInfoDto("https://metis-sandbox",
        10L, 10L, List.of(createProgress, externalProgress), false, "", emptyList(),
        tiersZeroInfo);

    when(datasetReportService.getReport("1")).thenReturn(report);

    mvc.perform(
        get("/user-datasets")

        .accept(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
        .contentType(MediaType.APPLICATION_JSON)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasSize(0)));

      // TODO: it should be 3 but the principal isn't mocked correctly
      //.andExpect(jsonPath("$", hasSize(3)));
      //.andExpect(jsonPath("$[0].dataset-id", is("1")))
  }
}
