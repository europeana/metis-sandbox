package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.patternanalysis.view.ProblemPattern;

import java.util.List;

/**
 * The type Validation workflow report.
 */
public class ValidationWorkflowReport {
    private final List<ValidationResult> validationResults;
    private final List<ProblemPattern> problemPatternList;

    /**
     * Instantiates a new Validation workflow report.
     *
     * @param validationResults the validation results
     * @param problemPatterns   the problem patterns
     */
    public ValidationWorkflowReport(List<ValidationResult> validationResults, List<ProblemPattern> problemPatterns) {
        this.validationResults = validationResults;
        this.problemPatternList = problemPatterns;
    }

    /**
     * Gets validation results.
     *
     * @return the validation results
     */
    public List<ValidationResult> getValidationResults() {
        return validationResults;
    }

    /**
     * Gets problem pattern list.
     *
     * @return the problem pattern list
     */
    public List<ProblemPattern> getProblemPatternList() {
        return problemPatternList;
    }
}
