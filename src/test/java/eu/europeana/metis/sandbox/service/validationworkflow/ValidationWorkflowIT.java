package eu.europeana.metis.sandbox.service.validationworkflow;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class ValidationWorkflowIT {

    @Test
    void validation_expectSuccess() throws IOException {
        Path testRecordPath = Paths.get("src", "test", "resources", "record", "validation", "valid_record.xml");
        MockMultipartFile mockMultipartFile = new MockMultipartFile("valid_record",
                "valid_record.xml", "application/rdf+xml", Files.newInputStream(testRecordPath));
    }
}
