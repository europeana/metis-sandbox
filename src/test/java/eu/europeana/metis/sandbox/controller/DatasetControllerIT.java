package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.common.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = SandboxApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
    locations = "classpath:application-test.yml")
@ActiveProfiles(value = "dev")
class DatasetControllerIT {

  @Autowired
  private MockMvc mvc;

  TestUtils testUtils;

  @BeforeEach
  void setUp() {
    testUtils = new TestUtils();
  }

  @Test
  public void processDataset_expectStatus_accepted() throws Exception {

    MockMultipartFile dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        testUtils.readFileToBytes("zip/dataset-valid.zip"));

    mvc.perform(multipart("/dataset/{name}/process", "my-data-set")
        .file(dataset)
        .param("country", ITALY.name())
        .param("language", IT.name()))
        .andExpect(status().isAccepted());
  }

  @Test
  public void retrieveDataset_expectStatus_ok() throws Exception {
    mvc.perform(get("/dataset/{id}", "my-data-set"))
        .andExpect(status().isOk());
  }

}