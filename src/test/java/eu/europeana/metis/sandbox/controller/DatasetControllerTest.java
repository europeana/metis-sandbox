package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.indexing.tiers.view.ContentTierBreakdown;
import eu.europeana.indexing.tiers.view.RecordTierCalculationSummary;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.TierStatistics;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfo;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService;
import eu.europeana.metis.sandbox.service.workflow.HarvestPublishService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;


@ExtendWith(SpringExtension.class)
@WebMvcTest(DatasetController.class)
class DatasetControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DatasetService datasetService;

    @MockBean
    private DatasetReportService datasetReportService;

    @MockBean
    private RecordLogService recordLogService;

    @MockBean
    private RecordTierCalculationService recordTierCalculationService;

    @MockBean
    private HarvestPublishService harvestPublishService;

    @Test
    void processDatasetFromZipFile_withoutXsltFile_expectSuccess() throws Exception {

        MockMultipartFile mockMultipart = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                "<test></test>".getBytes());

        when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT), any(ByteArrayInputStream.class)))
                .thenReturn("12345");

        mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
                        .file(mockMultipart)
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("stepsize","2"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.dataset-id", is("12345")));
    }

    @Test
    void processDatasetFromZipFile_withXsltFile_expectSuccess() throws Exception {

        MockMultipartFile mockMultipart = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                "<test></test>".getBytes());

        MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
                "application/xslt+xml",
                "string".getBytes());

        when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT), any(ByteArrayInputStream.class)))
                .thenReturn("12345");

        mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
                        .file(mockMultipart)
                        .file(xsltMock)
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("stepsize", "2"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.dataset-id", is("12345")));
    }

    @Test
    void processDatasetFromURL_withoutXsltFile_expectSuccess() throws Exception {

        String url = Paths.get("zip", "dataset-valid.zip").toUri().toString();

        when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT), any(ByteArrayInputStream.class)))
                .thenReturn("12345");

        mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("url", url)
                        .param("stepsize", "2"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.dataset-id", is("12345")));
    }

    @Test
    void processDatasetFromURL_withXsltFile_expectSuccess() throws Exception {

        final String url = Paths.get("zip", "dataset-valid.zip").toUri().toString();


        MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
                "application/xslt+xml",
                "string".getBytes());

        when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT), any(ByteArrayInputStream.class)))
                .thenReturn("12345");

        mvc.perform(multipart("/dataset/{name}/harvestByUrl", "my-data-set")
                        .file(xsltMock)
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("url", url)
                        .param("stepsize", "2"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.dataset-id", is("12345")));
    }

    @Test
    void processDatasetFromOAI_expectSuccess() throws Exception {

        final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

        when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT), any(ByteArrayInputStream.class)))
                .thenReturn("12345");

        mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
                        .param("country", ITALY.xmlValue())
                        .param("language", IT.xmlValue())
                        .param("url", url)
                        .param("setspec", "1073")
                        .param("metadataformat", "rdf")
                        .param("stepsize", "2"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.dataset-id", is("12345")));
    }

    @Test
    void processDatasetFromOAIWithXsltFile_expectSuccess() throws Exception {

        final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

        MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
                "application/xslt+xml",
                "string".getBytes());

        when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT), any(InputStream.class)))
                .thenReturn("12345");

        mvc.perform(multipart("/dataset/{name}/harvestOaiPmh", "my-data-set")
                        .file(xsltMock)
                        .param("country", ITALY.xmlValue())
                        .param("language", IT.xmlValue())
                        .param("url", url)
                        .param("setspec", "1073")
                        .param("metadataformat", "rdf")
                        .param("stepsize", "2"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.dataset-id", is("12345")));
    }

    @Test
    void processDatasetFromFile_invalidName_expectFail() throws Exception {

        var dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                "<test></test>".getBytes());

        mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data=set")
                        .file(dataset)
                        .param("country", ITALY.name())
                        .param("language", IT.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("dataset name can only include letters, numbers, _ or - characters")));
    }

    @Test
    void processDatasetFromFile_invalidStepSize_expectFail() throws Exception {

        var dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                "<test></test>".getBytes());

        mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
                        .file(dataset)
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("stepsize", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("Step size must be a number higher than zero")));
    }

    @Test
    void processDatasetFromURL_invalidName_expectFail() throws Exception {

        final String url = "zip" + File.separator + "dataset-valid.zip";

        mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data=set")
                        .param("name", "invalidDatasetName")
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("url", url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("dataset name can only include letters, numbers, _ or - characters")));
    }

    @Test
    void processDatasetFromURL_invalidStepSize_expectFail() throws Exception {

        final String url = "zip" + File.separator + "dataset-valid.zip";

        mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
                        .param("name", "invalidDatasetName")
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("url", url)
                        .param("stepsize", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("Step size must be a number higher than zero")));
    }

    @Test
    void processDatasetFromOAI_invalidName_expectFail() throws Exception {

        final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

        mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data=set")
                        .param("name", "invalidDatasetName")
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("url", url)
                        .param("setspec", "1073")
                        .param("metadataformat", "rdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("dataset name can only include letters, numbers, _ or - characters")));
    }

    @Test
    void processDatasetFromOAI_invalidStepSize_expectFail() throws Exception {

        final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

        mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
                        .param("name", "invalidDatasetName")
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("url", url)
                        .param("setspec", "1073")
                        .param("metadataformat", "rdf")
                        .param("stepsize", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("Step size must be a number higher than zero")));
    }

    @Test
    void processDatasetFromOAI_harvestServiceFails_expectFail() throws Exception {

        final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

        when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT), any(InputStream.class)))
                .thenReturn("12345");
        doThrow(new IllegalArgumentException(new Exception())).when(harvestPublishService)
                .runHarvestOaiPmhAsync(any(DatasetMetadata.class),
                        any(OaiHarvestData.class));

        mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("url", url)
                        .param("setspec", "1073")
                        .param("metadataformat", "rdf"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException));
    }

    @Test
    void processDatasetFromOAI_datasetServiceFails_expectFail() throws Exception {

        final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

        when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT), any(InputStream.class)))
                .thenThrow(new ServiceException("Failed", new Exception()));

        mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("url", url)
                        .param("setspec", "1073")
                        .param("metadataformat", "rdf"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message",
                        is("Failed Please retry, if problem persists contact provider.")));
    }


    @Test
    void processDatasetFromOAI_differentXsltFileType_expectFail() throws Exception {

        final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

        MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl", "application/zip",
                "string".getBytes());

        mvc.perform(multipart("/dataset/{name}/harvestOaiPmh", "my-data-set")
                        .file(xsltMock)
                        .param("country", ITALY.name())
                        .param("language", IT.name())
                        .param("url", url)
                        .param("setspec", "1073")
                        .param("metadataformat", "rdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("The given xslt file should be a single xml file.")));
    }

    @Test
    void retrieveDataset_expectSuccess() throws Exception {
        var message1 = "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.";
        var message2 = "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.";
        var error1 = new ErrorInfoDto(message1, Status.FAIL, List.of("1", "2"));
        var error2 = new ErrorInfoDto(message2, Status.FAIL, List.of("3", "4"));
        var errors = List.of(error1, error2);
        var createProgress = new ProgressByStepDto(Step.HARVEST_ZIP, 10, 0, 0, List.of());
        var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 7, 3, 0, errors);
        var datasetInfoDto = new DatasetInfoDto("12345", "Test", LocalDateTime.MIN, Language.NL,
                Country.NETHERLANDS, false, false);
        var tiersZeroInfo = new TiersZeroInfo(new TierStatistics(0, Collections.emptyList()),
                new TierStatistics(0, Collections.emptyList()));
        var report = new ProgressInfoDto("https://metis-sandbox",
                10L, 10L, List.of(createProgress, externalProgress), datasetInfoDto, "", tiersZeroInfo);
        when(datasetReportService.getReport("1")).thenReturn(report);

        mvc.perform(get("/dataset/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status",
                        is("COMPLETED")))
                .andExpect(jsonPath("$.progress-by-step[1].errors[0].message",
                        is(message1)))
                .andExpect(jsonPath("$.dataset-info.dataset-id", is("12345")))
                .andExpect(jsonPath("$.dataset-info.dataset-name", is("Test")))
                .andExpect(jsonPath("$.dataset-info.creation-date", is("-999999999-01-01T00:00:00")))
                .andExpect(jsonPath("$.dataset-info.language", is("Dutch")))
                .andExpect(jsonPath("$.dataset-info.country", is("Netherlands")))
                .andExpect(jsonPath("$.dataset-info.record-limit-exceeded", is(false)))
                .andExpect(jsonPath("$.dataset-info.transformed-to-edm-external", is(false)));
    }

    @Test
    void retrieveDataset_datasetInvalidDatasetId_expectFail() throws Exception {

        when(datasetReportService.getReport("1"))
                .thenThrow(new InvalidDatasetException("1"));

        mvc.perform(get("/dataset/{id}", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("Provided dataset id: [1] is not valid. ")));
    }

    @Test
    void retrieveDataset_datasetReportServiceFails_expectFail() throws Exception {

        when(datasetReportService.getReport("1"))
                .thenThrow(new ServiceException("Failed", new Exception()));

        mvc.perform(get("/dataset/{id}", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message",
                        is("Failed Please retry, if problem persists contact provider.")));
    }

    @Test
    void computeRecordTierCalculation_expectSuccess() throws Exception {
        final String datasetId = "1";
        final String recordId = "recordId";
        final String europeanaId = "europeanaId";

        final RecordTierCalculationSummary recordTierCalculationSummary = new RecordTierCalculationSummary();
        recordTierCalculationSummary.setEuropeanaRecordId(europeanaId);
        final RecordTierCalculationView recordTierCalculationView = new RecordTierCalculationView(recordTierCalculationSummary,
                new ContentTierBreakdown.Builder().build(), null);
        when(recordTierCalculationService.calculateTiers(recordId, datasetId)).thenReturn(recordTierCalculationView);

        mvc.perform(get("/dataset/{id}/record/compute-tier-calculation", datasetId)
                        .param("recordId", recordId))
                .andExpect(jsonPath("$.recordTierCalculationSummary.europeanaRecordId", is("europeanaId")))
                .andExpect(jsonPath("$.recordTierCalculationSummary.contentTier", isEmptyOrNullString()));
    }

    @Test
    void computeRecordTierCalculation_NoRecordFoundException() throws Exception {
        final String datasetId = "1";
        final String recordId = "recordId";
        when(recordTierCalculationService.calculateTiers(anyString(), anyString())).thenThrow(
                new NoRecordFoundException("record not found"));
        mvc.perform(get("/dataset/{id}/record/compute-tier-calculation", datasetId)
                        .param("recordId", recordId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message",
                        is("record not found")));
    }

    @Test
    void getRecord_expectSuccess() throws Exception {
        final String datasetId = "1";
        final String recordId = "europeanaId";
        final String returnString = "exampleString";
        when(recordLogService.getProviderRecordString(recordId, datasetId)).thenReturn(returnString);

        mvc.perform(get("/dataset/{id}/record", datasetId)
                        .param("recordId", recordId))
                .andExpect(content().string(returnString));
    }

    @Test
    void getRecord_NoRecordFoundException() throws Exception {
        final String datasetId = "1";
        final String recordId = "europeanaId";
        when(recordLogService.getProviderRecordString(anyString(), anyString())).thenThrow(
                new NoRecordFoundException("record not found"));

        mvc.perform(get("/dataset/{id}/record", datasetId).param("recordId", recordId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message",
                        is("record not found")));
    }
}
