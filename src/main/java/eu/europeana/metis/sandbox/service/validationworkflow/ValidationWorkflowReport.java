package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.patternanalysis.view.ProblemPattern;

import java.util.List;

public class ValidationWorkflowReport {
    private final ValidationResult result;
    private final List<ProblemPattern> problemPatternList;

    public ValidationWorkflowReport(ValidationResult validationResult, List<ProblemPattern> problemPatterns) {
        this.result = validationResult;
        this.problemPatternList = problemPatterns;
    }

    public ValidationResult getResult() {
        return result;
    }

    public List<ProblemPattern> getProblemPatternList() {
        return problemPatternList;
    }
}
