package eu.europeana.metis.sandbox.controller;

import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.common.TestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = SandboxApplication.class)
@AutoConfigureMockMvc
class DatasetControllerIT {

  private final TestUtils testUtils = new TestUtils();

  @Autowired
  private MockMvc mvc;

  @Test
  public void processDatasetFromFile_expectStatus_accepted() throws Exception {

    MockMultipartFile dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        testUtils.readFileToBytes("zip" + File.separator + "dataset-valid.zip"));

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
            .file(dataset)
            .param("country", ITALY.xmlValue())
            .param("language", IT.xmlValue()))
        .andExpect(status().isAccepted());
  }

  @Test
  public void processDatasetFromUrl_expectStatus_accepted() throws Exception {

    Path datasetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(datasetPath));

    mvc.perform(multipart("/dataset/{name}/harvestByUrl", "my-data-set")
            .param("country", ITALY.xmlValue())
            .param("language", IT.xmlValue())
            .param("url", datasetPath.toString()))
        .andExpect(status().isAccepted());
  }


  @Test
  public void retrieveDataset_expectStatus_ok() throws Exception {
    mvc.perform(get("/dataset/{id}", "1"))
        .andExpect(status().isOk());
  }

}