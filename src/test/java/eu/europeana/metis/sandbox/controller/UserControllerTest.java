package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static eu.europeana.metis.security.test.JwtUtils.BEARER;
import static eu.europeana.metis.security.test.JwtUtils.MOCK_VALID_TOKEN;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.config.SecurityConfig;
import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
import eu.europeana.metis.sandbox.controller.advice.RestResponseExceptionHandler;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.DatasetWithExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressByStepDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionStatus;
import eu.europeana.metis.sandbox.dto.report.TierStatisticsDTO;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfoDTO;
import eu.europeana.metis.sandbox.service.user.UserService;
import eu.europeana.metis.security.test.JwtUtils;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {WebMvcConfig.class, RestResponseExceptionHandler.class, SecurityConfig.class,
    UserController.class})
class UserControllerTest {

  private static final String DATASET_ID = "datasetId";

  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private UserService userService;
  private static MockMvc mockMvc;
  private final JwtUtils jwtUtils = new JwtUtils(List.of());

  @BeforeAll
  static void setup(WebApplicationContext context) {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .apply(SecurityMockMvcConfigurers.springSecurity())
                             .defaultRequest(get("/"))
                             .build();
  }

  @Test
  void getUserDatasets() throws Exception {
    Jwt jwt = setupJwt();

    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "steStep", "metadataFormat", 1);
    DatasetInfoDTO datasetInfoDTO = DatasetInfoDTO.builder()
                                                  .datasetId(DATASET_ID)
                                                  .datasetName("datasetName")
                                                  .createdById("createdById")
                                                  .creationDate(ZonedDateTime.now())
                                                  .language(Language.EL)
                                                  .country(Country.GREECE)
                                                  .abstractHarvestParametersDTO(oaiHarvestParametersDTO)
                                                  .transformedToEdmExternal(false)
                                                  .build();

    ExecutionProgressByStepDTO executionProgressByStepDto = new ExecutionProgressByStepDTO(
        FullBatchJobType.HARVEST_OAI, 10, 0, 0, 0, List.of()
    );
    TiersZeroInfoDTO tiersZeroInfoDTO = new TiersZeroInfoDTO(new TierStatisticsDTO(0, List.of()),
        new TierStatisticsDTO(0, List.of()));

    ExecutionProgressInfoDTO executionProgressInfoDTO = new ExecutionProgressInfoDTO(
        "publishPortalUrl",
        ExecutionStatus.COMPLETED,
        10,
        10,
        List.of(executionProgressByStepDto),
        false,
        tiersZeroInfoDTO
    );

    DatasetWithExecutionProgressInfoDTO datasetWithExecutionProgressInfoDTO =
        new DatasetWithExecutionProgressInfoDTO(datasetInfoDTO, executionProgressInfoDTO);

    when(userService.getDatasetInfoWithProgressOwnedByUserId(getUserId(jwt))).thenReturn(
        List.of(datasetWithExecutionProgressInfoDTO));

    mockMvc.perform(get("/users/me/datasets")
               .headers(getCommonAuthorizationUserAgentHeaders()))
           .andExpect(status().isOk())
           .andExpect(content().contentType("application/json"))
           .andExpect(jsonPath("$[0].dataset-info").exists())
           .andExpect(jsonPath("$[0].dataset-info.dataset-id", is(datasetInfoDTO.getDatasetId())))

           .andExpect(jsonPath("$[0].execution-progress-info").exists())
           .andExpect(jsonPath("$[0].execution-progress-info.status", is(executionProgressInfoDTO.executionStatus().name())))
           .andExpect(jsonPath("$[0].execution-progress-info.total-records",
               is(Math.toIntExact(executionProgressInfoDTO.totalRecords()))))
           .andExpect(
               jsonPath("$[0].execution-progress-info.processed-records",
                   is(Math.toIntExact(executionProgressInfoDTO.processedRecords()))));

  }

  @Test
  void getUserDatasetsExpectEmpty() throws Exception {
    Jwt jwt = setupJwt();
    when(userService.getDatasetInfoWithProgressOwnedByUserId(getUserId(jwt))).thenReturn(List.of());

    mockMvc.perform(get("/users/me/datasets")
               .headers(getCommonAuthorizationUserAgentHeaders()))
           .andExpect(status().isOk())
           .andExpect(content().contentType("application/json"))
           .andExpect(jsonPath("$", hasSize(0)));

    //And without authorization header
    mockMvc.perform(get("/users/me/datasets"))
           .andExpect(status().isOk())
           .andExpect(content().contentType("application/json"))
           .andExpect(jsonPath("$", hasSize(0)));
  }

  private Jwt setupJwt() {
    Jwt jwt = jwtUtils.getEmptyRoleJwt();
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwt);
    return jwt;
  }

  private HttpHeaders getCommonAuthorizationUserAgentHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, BEARER + MOCK_VALID_TOKEN);
    headers.add(HttpHeaders.USER_AGENT, "Mozilla");
    return headers;
  }
}
