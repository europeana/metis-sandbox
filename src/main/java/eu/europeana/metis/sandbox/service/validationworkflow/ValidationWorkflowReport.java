package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.patternanalysis.view.ProblemPattern;

import java.util.List;

public class ValidationWorkflowReport {
    private final List<ValidationResult> validationResults;
    private final List<ProblemPattern> problemPatternList;

    public ValidationWorkflowReport(List<ValidationResult> validationResults, List<ProblemPattern> problemPatterns) {
        this.validationResults = validationResults;
        this.problemPatternList = problemPatterns;
    }

    public List<ValidationResult> getValidationResults() {
        return validationResults;
    }

    public List<ProblemPattern> getProblemPatternList() {
        return problemPatternList;
    }
}
