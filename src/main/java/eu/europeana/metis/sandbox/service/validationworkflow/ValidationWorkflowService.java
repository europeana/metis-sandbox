package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.ProblemPatternAnalyzer;
import eu.europeana.patternanalysis.view.ProblemPatternAnalysis;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The type Validation workflow service.
 */
@Service
public class ValidationWorkflowService {

    private final ValidationStep validationStep;
    private final ProblemPatternAnalyzer problemPatternAnalyzer;

    /**
     * Instantiates a new Validation workflow service.
     *
     * @param harvestValidationStep            the harvest validation step
     * @param externalValidationStep           the external validation step
     * @param transformationValidationStep     the transformation validation step
     * @param internalValidationValidationStep the internal validation validation step
     */
    public ValidationWorkflowService(ValidationStep harvestValidationStep,
                                     ValidationStep externalValidationStep,
                                     ValidationStep transformationValidationStep,
                                     ValidationStep internalValidationValidationStep,
                                     ProblemPatternAnalyzer problemPatternAnalyzer) {
        this.problemPatternAnalyzer = problemPatternAnalyzer;
        this.validationStep = harvestValidationStep;
        // set the chain of responsibility
        this.validationStep.setNextValidationStep(externalValidationStep);
        externalValidationStep.setNextValidationStep(transformationValidationStep);
        transformationValidationStep.setNextValidationStep(internalValidationValidationStep);
        internalValidationValidationStep.setNextValidationStep(null);
    }

    /**
     * Validate validation worflow report.
     *
     * @param recordToValidate the record to validate
     * @param country          the country
     * @param language         the language
     * @return the validation worflow report
     * @throws SerializationException the serialization exception
     * @throws IOException            the io exception
     */
    public ValidationWorkflowReport validate(MultipartFile recordToValidate, Country country, Language language) throws SerializationException, IOException {

        final Record harvestedRecord = Record.builder()
                .content(recordToValidate.getInputStream().readAllBytes())
                .country(country)
                .language(language)
                .build();
        ValidationResult validationResult = this.validationStep.validate(harvestedRecord);

        final ProblemPatternAnalysis patternAnalysis = this.problemPatternAnalyzer.analyzeRecord(new String(harvestedRecord.getContent(), StandardCharsets.UTF_8));
        return new ValidationWorkflowReport(validationResult, patternAnalysis.getProblemPatterns());
    }
}
