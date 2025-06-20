package eu.europeana.metis.sandbox.controller;

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
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressByStepDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionStatus;
import eu.europeana.metis.sandbox.dto.report.TierStatisticsDTO;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfoDTO;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(DatasetInfoController.class)
@ContextConfiguration(classes = {WebMvcConfig.class, RestResponseExceptionHandler.class, SecurityConfig.class,
    DatasetInfoController.class})
class DatasetInfoControllerTest {

  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private DatasetReportService datasetReportService;
  private static MockMvc mockMvc;

  @BeforeAll
  static void setup(WebApplicationContext context) {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .apply(SecurityMockMvcConfigurers.springSecurity())
                             .defaultRequest(get("/"))
                             .build();
  }

  @Test
  void getDatasetProgress() throws Exception {
    String datasetId = "datasetId";

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

    when(datasetReportService.getProgress(datasetId)).thenReturn(executionProgressInfoDTO);

    mockMvc.perform(get("/dataset/{id}/progress", datasetId))
           .andExpect(status().isOk())
           .andExpect(content().contentType("application/json"))
           .andExpect(jsonPath("$.total-records", is(10)))
           .andExpect(jsonPath("$.status", is(ExecutionStatus.COMPLETED.name())))
           .andExpect(jsonPath("$.progress-by-step[0].step", is(FullBatchJobType.HARVEST_OAI.name())))
           .andExpect(jsonPath("$.tier-zero-info.content-tier.total", is(0)));
  }

  @Test
  void getDatasetInfo() throws Exception {
    String datasetId = "datasetId";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "steStep", "metadataFormat", 1);
    DatasetInfoDTO datasetInfoDTO = DatasetInfoDTO.builder()
                                                  .datasetId(datasetId)
                                                  .datasetName("datasetName")
                                                  .createdById("createdById")
                                                  .creationDate(ZonedDateTime.now())
                                                  .language(Language.EL)
                                                  .country(Country.GREECE)
                                                  .abstractHarvestParametersDTO(oaiHarvestParametersDTO)
                                                  .transformedToEdmExternal(false)
                                                  .build();

    when(datasetReportService.getDatasetInfo(datasetId)).thenReturn(datasetInfoDTO);

    mockMvc.perform(get("/dataset/{id}/info", datasetId))
           .andExpect(status().isOk())
           .andExpect(content().contentType("application/json"))
           .andExpect(jsonPath("$.dataset-id", is(datasetId)))
           .andExpect(jsonPath("$.language", is(Language.EL.xmlValue())))
           .andExpect(jsonPath("$.country", is(Country.GREECE.xmlValue())))
           .andExpect(jsonPath("$.harvesting-parameters.url", is(oaiHarvestParametersDTO.getUrl())))
           .andExpect(jsonPath("$.harvesting-parameters.step-size", is(oaiHarvestParametersDTO.getStepSize())));
  }

  @Test
  void getAllCountries() throws Exception {
    List<Country> countries = Country.getCountryListSortedByName();

    ResultActions resultActions = mockMvc.perform(get("/dataset/countries"))
                                         .andExpect(status().isOk())
                                         .andExpect(content().contentType("application/json"))
                                         .andExpect(jsonPath("$", hasSize(countries.size())))
                                         .andExpect(jsonPath("$[0].name", is(countries.getFirst().name())))
                                         .andExpect(jsonPath("$[0].xmlValue", is(countries.getFirst().xmlValue())));

    for (int i = 0; i < countries.size(); i++) {
      Country country = countries.get(i);
      resultActions
          .andExpect(jsonPath("$[" + i + "].name", is(country.name())))
          .andExpect(jsonPath("$[" + i + "].xmlValue", is(country.xmlValue())));
    }
  }

  @Test
  void getAllLanguages() throws Exception {
    List<Language> languages = Language.getLanguageListSortedByName();

    ResultActions resultActions = mockMvc.perform(get("/dataset/languages"))
                                         .andExpect(status().isOk())
                                         .andExpect(content().contentType("application/json"))
                                         .andExpect(jsonPath("$", hasSize(languages.size())))
                                         .andExpect(jsonPath("$[0].name", is(languages.getFirst().name())))
                                         .andExpect(jsonPath("$[0].xmlValue", is(languages.getFirst().xmlValue())));

    for (int i = 0; i < languages.size(); i++) {
      Language language = languages.get(i);
      resultActions
          .andExpect(jsonPath("$[" + i + "].name", is(language.name())))
          .andExpect(jsonPath("$[" + i + "].xmlValue", is(language.xmlValue())));
    }
  }
}
