package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.workflow.HarvestService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
  private HarvestService harvestService;

  @MockBean
  private DatasetService datasetService;

  @MockBean
  private DatasetReportService datasetReportService;

  private final TestUtils testUtils = new TestUtils();

  @Test
  void processDatasetFromFile_expectSuccess() throws Exception {

    var dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        "<test></test>".getBytes());

    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));

    var datasetObject = new Dataset("12345", Set.of(), 0);

    when(harvestService.harvestZipMultipartFile(dataset)).thenReturn(records);
    when(datasetService.createDataset("my-data-set", ITALY, IT, records)).thenReturn(datasetObject);

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
            .file(dataset)
            .param("country", ITALY.name())
            .param("language", IT.name()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @Test
  void processDatasetFromURL_expectSuccess() throws Exception {

    String url = "zip" + File.separator + "dataset-valid.zip";

    var records = List.of(new ByteArrayInputStream(testUtils.readFileToBytes(url)));

    var datasetObject = new Dataset("12345", Set.of(), 0);

    when(harvestService.harvestZipUrl(url)).thenReturn(records);
    when(datasetService.createDataset("my-data-set", ITALY, IT, records)).thenReturn(datasetObject);

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @Test
  void processDatasetFromOAI_expectSuccess() throws Exception {

    String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));
    ImmutablePair<AtomicBoolean, List<ByteArrayInputStream>> pairMock =
        new ImmutablePair<>(new AtomicBoolean(false), records);

    var datasetObject = new Dataset("12345", Set.of(), 0);

    when(harvestService.harvestOaiPmhEndpoint(url, "1073", "rdf")).thenReturn(pairMock);
    when(datasetService.createDataset("my-data-set", ITALY, IT, records)).thenReturn(datasetObject);

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
            .param("country", ITALY.xmlValue())
            .param("language", IT.xmlValue())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf"))
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
  void processDatasetFromURL_invalidName_expectFail() throws Exception {

    String url = "zip" + File.separator + "dataset-valid.zip";

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
  void processDatasetFromOAI_invalidName_expectFail() throws Exception {

    String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

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
  void processDatasetFromFile_harvestServiceFails_expectFail() throws Exception {

    var dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        "<test></test>".getBytes());

    when(harvestService.harvestZipMultipartFile(dataset)).thenThrow(new InvalidZipFileException(new Exception()));

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
            .file(dataset)
            .param("country", ITALY.name())
            .param("language", IT.name()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("File provided is not valid zip. ")));
  }

  @Test
  void processDatasetFromURL_harvestServiceFails_expectFail() throws Exception {

    String url = "zip" + File.separator + "dataset-valid.zip";

    when(harvestService.harvestZipUrl(url)).thenThrow(new IllegalArgumentException(new Exception()));

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url))
        .andExpect(status().isBadRequest())
        .andExpect(result -> assertTrue(
            result.getResolvedException() instanceof IllegalArgumentException));
  }

  @Test
  void processDatasetFromOAI_harvestServiceFails_expectFail() throws Exception {

    String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    when(harvestService.harvestOaiPmhEndpoint(url, "1073", "rdf")).thenThrow(
        new IllegalArgumentException(new Exception()));

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
  void processDatasetFromFile_recordsQtyExceeded_expectFail() throws Exception {

    var records = IntStream.range(0, 1000)
        .boxed()
        .map(Object::toString)
        .map(String::getBytes)
        .map(ByteArrayInputStream::new)
        .collect(Collectors.toList());

    var dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        "<test></test>".getBytes());

    when(harvestService.harvestZipMultipartFile(dataset)).thenReturn(records);

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
            .file(dataset)
            .param("country", ITALY.name())
            .param("language", IT.name()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            containsString("Amount of records can not be more than")));
  }

  @Test
  void processDatasetFromURL_recordsQtyExceeded_expectFail() throws Exception {

    String url = "zip" + File.separator + "dataset-valid.zip";

    var records = IntStream.range(0, 1000)
        .boxed()
        .map(Object::toString)
        .map(String::getBytes)
        .map(ByteArrayInputStream::new)
        .collect(Collectors.toList());

    when(harvestService.harvestZipUrl(url)).thenReturn(records);

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            containsString("Amount of records can not be more than")));
  }

  @Test
  void processDatasetFromOAI_recordsQtyExceeded_expectFail() throws Exception {

    String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    var records = IntStream.range(0, 1000)
        .boxed()
        .map(Object::toString)
        .map(String::getBytes)
        .map(ByteArrayInputStream::new)
        .collect(Collectors.toList());
    ImmutablePair<AtomicBoolean, List<ByteArrayInputStream>> pairMock =
        new ImmutablePair<>(new AtomicBoolean(false), records);

    when(harvestService.harvestOaiPmhEndpoint(url, "1073", "rdf")).thenReturn(pairMock);

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            containsString("Amount of records can not be more than")));
  }

  @Test
  void processDatasetFromFile_datasetServiceFails_expectFail() throws Exception {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));

    var dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        "<test></test>".getBytes());

    when(harvestService.harvestZipMultipartFile(dataset)).thenReturn(records);
    when(datasetService.createDataset("my-data-set", ITALY, IT, records))
        .thenThrow(new ServiceException("Failed", new Exception()));

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
            .file(dataset)
            .param("country", ITALY.name())
            .param("language", IT.name()))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message",
            is("Failed Please retry, if problem persists contact provider.")));
  }

  @Test
  void processDatasetFromURL_datasetServiceFails_expectFail() throws Exception {

    String url = "zip" + File.separator + "dataset-valid.zip";
    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));

    when(harvestService.harvestZipUrl(url)).thenReturn(records);
    when(datasetService.createDataset("my-data-set", ITALY, IT, records))
        .thenThrow(new ServiceException("Failed", new Exception()));

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message",
            is("Failed Please retry, if problem persists contact provider.")));
  }

  @Test
  void processDatasetFromOAI_datasetServiceFails_expectFail() throws Exception {

    String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();
    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));
    ImmutablePair<AtomicBoolean, List<ByteArrayInputStream>> pairMock =
        new ImmutablePair<>(new AtomicBoolean(false), records);

    when(harvestService.harvestOaiPmhEndpoint(url, "1073", "rdf")).thenReturn(pairMock);
    when(datasetService.createDataset("my-data-set", ITALY, IT, records))
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
  void processDatasetFromFile_datasetServiceInvalidRecord_expectFail() throws Exception {

    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));

    var dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        "<test></test>".getBytes());

    when(harvestService.harvestZipMultipartFile(dataset)).thenReturn(records);
    when(datasetService.createDataset("my-data-set", ITALY, IT, records))
        .thenThrow(new RecordParsingException(new Exception()));

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
            .file(dataset)
            .param("country", ITALY.name())
            .param("language", IT.name()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("Error while parsing a xml record. ")));
  }

  @Test
  void processDatasetFromURL_datasetServiceInvalidRecord_expectFail() throws Exception {

    String url = "zip" + File.separator + "dataset-valid.zip";
    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));

    when(harvestService.harvestZipUrl(url)).thenReturn(records);
    when(datasetService.createDataset("my-data-set", ITALY, IT, records))
        .thenThrow(new RecordParsingException(new Exception()));

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("Error while parsing a xml record. ")));
  }

  @Test
  void processDatasetFromOAI_datasetServiceInvalidRecord_expectFail() throws Exception {

    String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();
    var records = List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()));
    ImmutablePair<AtomicBoolean, List<ByteArrayInputStream>> pairMock =
        new ImmutablePair<>(new AtomicBoolean(false), records);

    when(harvestService.harvestOaiPmhEndpoint(url, "1073", "rdf")).thenReturn(pairMock);
    when(datasetService.createDataset("my-data-set", ITALY, IT, records))
        .thenThrow(new RecordParsingException(new Exception()));

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("Error while parsing a xml record. ")));
  }

  @Test
  void retrieveDataset_expectSuccess() throws Exception {
    var message1 = "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.";
    var message2 = "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.";
    var error1 = new ErrorInfoDto(message1, Status.FAIL, List.of("1", "2"));
    var error2 = new ErrorInfoDto(message2, Status.FAIL, List.of("3", "4"));
    var errors = List.of(error1, error2);
    var createProgress = new ProgressByStepDto(Step.CREATE, 10, 0, 0, List.of());
    var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 7, 3, 0, errors);
    var datasetInfoDto = new DatasetInfoDto("12345", "Test", LocalDateTime.MIN, Language.NL,
        Country.NETHERLANDS);
    var report = new ProgressInfoDto("https://metis-sandbox", "https://metis-sandbox",
        10, 10L, List.of(createProgress, externalProgress), datasetInfoDto);
    when(datasetReportService.getReport("1")).thenReturn(report);

    mvc.perform(get("/dataset/{id}", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status",
            is("completed")))
        .andExpect(jsonPath("$.portal-preview",
            is("https://metis-sandbox")))
        .andExpect(jsonPath("$.progress-by-step[1].errors[0].message",
            is(message1)))
        .andExpect(jsonPath("$.dataset-info.dataset-id", is("12345")))
        .andExpect(jsonPath("$.dataset-info.dataset-name", is("Test")))
        .andExpect(jsonPath("$.dataset-info.creation-date", is("-999999999-01-01T00:00:00")))
        .andExpect(jsonPath("$.dataset-info.language", is("Dutch")))
        .andExpect(jsonPath("$.dataset-info.country", is("Netherlands")));
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
}