package eu.europeana.metis.sandbox.dto.validation;

import eu.europeana.patternanalysis.view.ProblemPattern;
import java.util.List;

/**
 * Represents the report generated after executing a direct validation workflow.
 *
 * <p>Encapsulates the results of the steps in the direct validation workflow and the associated problem patterns.
 */
public record ValidationWorkflowReport(List<ValidationResult> validationResults,
                                       List<ProblemPattern> problemPatternList) {

}
