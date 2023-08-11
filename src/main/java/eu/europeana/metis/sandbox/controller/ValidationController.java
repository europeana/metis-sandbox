package eu.europeana.metis.sandbox.controller;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationWorkflowReport;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationWorkflowService;
import eu.europeana.metis.schema.convert.SerializationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * The type Validation controller.
 */
@RestController
@RequestMapping("/record/")
@Tag(name = "Record validation controller")
public class ValidationController {

    private final ValidationWorkflowService workflowService;

    /**
     * Instantiates a new Validation controller.
     *
     * @param validationWorkflowService the validation workflow service
     */
    public ValidationController(ValidationWorkflowService validationWorkflowService) {
        this.workflowService = validationWorkflowService;
    }

    /**
     * Validate validation workflow report.
     *
     * @param country          the country
     * @param language         the language
     * @param recordToValidate the record to validate
     * @return the validation workflow report
     * @throws SerializationException the serialization exception
     * @throws IOException            the io exception
     */
    @Operation(summary = "Validate a record in rdf+xml format", description = "Validation & field warnings")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Error")
    @PostMapping(value = "/validation", produces = APPLICATION_JSON_VALUE, consumes = MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = {@Content(mediaType = MULTIPART_FORM_DATA_VALUE)})
    @ResponseStatus(HttpStatus.OK)
    public ValidationWorkflowReport validate(
            @Parameter(description = "country of the record") @RequestParam Country country,
            @Parameter(description = "language of the record") @RequestParam Language language,
            @Parameter(description = "record file to be validated", required = true) @RequestParam MultipartFile recordToValidate) throws SerializationException, IOException {

        return workflowService.validate(recordToValidate, country, language);
    }
}
