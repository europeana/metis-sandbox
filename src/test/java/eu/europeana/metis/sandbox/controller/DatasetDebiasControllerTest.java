//package eu.europeana.metis.sandbox.controller;
//
//import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
//import static eu.europeana.metis.sandbox.common.locale.Language.IT;
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//import eu.europeana.metis.sandbox.config.SecurityConfig;
//import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
//import eu.europeana.metis.sandbox.controller.advice.ControllerErrorHandler;
//import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
//import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
//import eu.europeana.metis.sandbox.dto.FileHarvestingDto;
//import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDto;
//import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDto;
//import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
//import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
//import eu.europeana.metis.sandbox.service.dataset.DatasetService;
//import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
//import java.time.Instant;
//import java.time.ZoneOffset;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.concurrent.locks.ReentrantLock;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.integration.support.locks.LockRegistry;
//import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//
//@WebMvcTest(DatasetDebiasController.class)
//@ContextConfiguration(classes = {WebMvcConfig.class, DatasetDebiasController.class, SecurityConfig.class,
//    ControllerErrorHandler.class})
//class DatasetDebiasControllerTest {
//
//  @MockBean
//  private DeBiasStateService deBiasStateService;
//
//  @MockBean
//  private DatasetService datasetService;
//
//  @MockBean
//  private DatasetReportService datasetReportService;
//
//  @MockBean
//  private RateLimitInterceptor rateLimitInterceptor;
//
//  @MockBean
//  private LockRegistry lockRegistry;
//
//  private static MockMvc mvc;
//
//  @BeforeAll
//  static void setup(WebApplicationContext context) {
//    mvc = MockMvcBuilders.webAppContextSetup(context)
//                         .apply(SecurityMockMvcConfigurers.springSecurity())
//                         .defaultRequest(get("/"))
//                         .build();
//  }
//
//  @ParameterizedTest
//  @ValueSource(strings = {"COMPLETED", "PROCESSING", "ERROR"})
//  void getDebiasStatus_expectSuccess(String status) throws Exception {
//    final Integer datasetId = 1;
//    final ZonedDateTime dateTime = ZonedDateTime.now();
//
//    when(deBiasStateService.getDeBiasStatus(datasetId))
//        .thenReturn(new DeBiasStatusDto(datasetId, status, dateTime, 1, 1));
//
//    mvc.perform(get("/dataset/{id}/debias/info", datasetId))
//       .andExpect(status().isOk())
//       .andExpect(jsonPath("$.dataset-id", is(datasetId)))
//       .andExpect(jsonPath("$.state", is(status)))
//       .andExpect(jsonPath("$.creation-date", is(dateTime.toOffsetDateTime()
//                                                         .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))))
//       .andExpect(jsonPath("$.total-records", is(1)))
//       .andExpect(jsonPath("$.processed-records", is(1)));
//  }
//
//  @ParameterizedTest
//  @ValueSource(strings = {"COMPLETED", "PROCESSING", "ERROR"})
//  void getDebiasReport_expectSuccess(String status) throws Exception {
//    final Integer datasetId = 1;
//    final ZonedDateTime dateTime = ZonedDateTime.now();
//
//    when(deBiasStateService.getDeBiasReport(datasetId))
//        .thenReturn(new DeBiasReportDto(datasetId, status, dateTime, 1, 1, List.of()));
//
//    mvc.perform(get("/dataset/{id}/debias/report", datasetId))
//       .andExpect(status().isOk())
//       .andExpect(jsonPath("$.dataset-id", is(datasetId)))
//       .andExpect(jsonPath("$.state", is(status)))
//       .andExpect(jsonPath("$.creation-date", is(dateTime.toOffsetDateTime()
//                                                         .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))))
//       .andExpect(jsonPath("$.total-records", is(1)))
//       .andExpect(jsonPath("$.processed-records", is(1)));
//  }
//
//  @ParameterizedTest
//  @ValueSource(booleans = {true, false})
//  void processDebias_expectSuccess(boolean process) throws Exception {
//    final Integer datasetId = 1;
//    Instant minInstant = Instant.ofEpochMilli(Long.MIN_VALUE);
//    ZonedDateTime mockTime = minInstant.atZone(ZoneOffset.UTC);
//    DatasetInfoDto mock = new DatasetInfoDto.Builder()
//        .datasetId("1")
//        .datasetName("datasetName")
//        .creationDate(mockTime)
//        .language(IT)
//        .country(ITALY)
//        .harvestingParametricDto(new FileHarvestingDto("fileName", "fileType"))
//        .transformedToEdmExternal(false)
//        .build();
//    when(datasetService.getDatasetInfo("1")).thenReturn(mock);
//
//    when(datasetReportService.getReport(datasetId.toString())).thenReturn(
//        new ProgressInfoDto("url", 1L, 1L,
//            List.of(), false, "", List.of(), null));
//    when(deBiasStateService.getDeBiasStatus(datasetId)).thenReturn(
//        new DeBiasStatusDto(datasetId, "READY", ZonedDateTime.now(), 1, 1));
//    when(deBiasStateService.process(datasetId)).thenReturn(process);
//    when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());
//    mvc.perform(post("/dataset/{id}/debias", datasetId))
//       .andExpect(status().isOk())
//       .andExpect(content().string(String.valueOf(process)));
//  }
//}
