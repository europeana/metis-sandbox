package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static eu.europeana.metis.security.test.JwtUtils.BEARER;
import static eu.europeana.metis.security.test.JwtUtils.MOCK_VALID_TOKEN;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.metis.sandbox.config.SecurityConfig;
import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
import eu.europeana.metis.sandbox.controller.advice.RestResponseExceptionHandler;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDTO;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDTO;
import eu.europeana.metis.sandbox.dto.debias.DebiasState;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.security.test.JwtUtils;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DatasetDebiasController.class)
@ContextConfiguration(classes = {WebMvcConfig.class, RestResponseExceptionHandler.class, SecurityConfig.class,
    DatasetDebiasController.class})
class DatasetDebiasControllerTest {

  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockitoBean
  JwtDecoder jwtDecoder;

  @MockitoBean
  private DatasetReportService datasetReportService;

  @MockitoBean
  private DatasetExecutionService datasetExecutionService;

  @MockitoBean
  private DeBiasStateService debiasStateService;

  @Autowired
  private MockMvc mockMvc;
  private final JwtUtils jwtUtils = new JwtUtils(List.of());

  @Test
  void processDebias_NoOwner_ReturnTrue() throws Exception {
    DatasetInfoDTO datasetInfoDTO = DatasetInfoDTO.builder().datasetId("1").build();
    when(datasetReportService.getDatasetInfo(datasetInfoDTO.getDatasetId())).thenReturn(datasetInfoDTO);
    when(datasetExecutionService.createAndExecuteDatasetForDebias(datasetInfoDTO.getDatasetId())).thenReturn(true);
    mockMvc.perform(post("/dataset/{id}/debias", datasetInfoDTO.getDatasetId())
               .accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(content().string("true"));
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

  @Test
  void processDeBias_ValidOwner_ReturnTrue() throws Exception {
    Jwt jwt = setupJwt();

    DatasetInfoDTO datasetInfoDTO = DatasetInfoDTO.builder().datasetId("1").createdById(getUserId(jwt)).build();
    when(datasetReportService.getDatasetInfo(String.valueOf(datasetInfoDTO.getDatasetId()))).thenReturn(datasetInfoDTO);
    when(datasetExecutionService.createAndExecuteDatasetForDebias(datasetInfoDTO.getDatasetId())).thenReturn(true);

    mockMvc.perform(post("/dataset/{id}/debias", datasetInfoDTO.getDatasetId())
               .headers(getCommonAuthorizationUserAgentHeaders())
               .accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(content().string("true"));
  }

  @Test
  void processDeBias_InvalidOwner_ReturnFalse() throws Exception {
    setupJwt();
    DatasetInfoDTO datasetInfoDTO = DatasetInfoDTO.builder().datasetId("1").createdById("some-user-id").build();
    when(datasetReportService.getDatasetInfo(String.valueOf(datasetInfoDTO.getDatasetId()))).thenReturn(datasetInfoDTO);

    mockMvc.perform(post("/dataset/{id}/debias", datasetInfoDTO.getDatasetId())
               .headers(getCommonAuthorizationUserAgentHeaders())
               .accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(content().string("false"));
    verifyNoInteractions(datasetExecutionService);
  }

  @Test
  void testProcessDeBias_NoJwtAndHasOwner_ReturnFalse() throws Exception {
    DatasetInfoDTO datasetInfoDTO = DatasetInfoDTO.builder().datasetId("1").createdById("some-user-id").build();
    when(datasetReportService.getDatasetInfo(String.valueOf(datasetInfoDTO.getDatasetId()))).thenReturn(datasetInfoDTO);

    mockMvc.perform(post("/dataset/{id}/debias", datasetInfoDTO.getDatasetId())
               .accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(content().string("false"));
    verifyNoInteractions(datasetExecutionService);
  }

  @Test
  void getDebiasReport_expectSuccess() throws Exception {
    final ZonedDateTime dateTime = ZonedDateTime.now();
    DeBiasReportDTO deBiasReportDTO = new DeBiasReportDTO(1, DebiasState.COMPLETED, dateTime, 1L, 1L, List.of());

    when(debiasStateService.getDeBiasReport(String.valueOf(deBiasReportDTO.getDatasetId()))).thenReturn(deBiasReportDTO);

    mockMvc.perform(get("/dataset/{id}/debias/report", deBiasReportDTO.getDatasetId())
               .accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.dataset-id", is(deBiasReportDTO.getDatasetId())))
           .andExpect(jsonPath("$.state", is(DebiasState.COMPLETED.name())))
           .andExpect(jsonPath("$.creation-date", is(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))))
           .andExpect(jsonPath("$.total-records", is(1)))
           .andExpect(jsonPath("$.processed-records", is(1)));
  }

  @Test
  void getDebiasStatus_expectSuccess() throws Exception {
    final ZonedDateTime dateTime = ZonedDateTime.now();
    DeBiasStatusDTO deBiasStatusDTO = new DeBiasStatusDTO(1, DebiasState.COMPLETED, dateTime, 1L, 1L);

    when(debiasStateService.getDeBiasStatus(String.valueOf(deBiasStatusDTO.getDatasetId()))).thenReturn(deBiasStatusDTO);

    mockMvc.perform(get("/dataset/{id}/debias/info", deBiasStatusDTO.getDatasetId())
               .accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.dataset-id", is(deBiasStatusDTO.getDatasetId())))
           .andExpect(jsonPath("$.state", is(DebiasState.COMPLETED.name())))
           .andExpect(jsonPath("$.creation-date", is(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))))
           .andExpect(jsonPath("$.total-records", is(1)))
           .andExpect(jsonPath("$.processed-records", is(1)));
  }
}
