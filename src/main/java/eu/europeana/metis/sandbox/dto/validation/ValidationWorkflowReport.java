package eu.europeana.metis.sandbox.dto.validation;

import eu.europeana.patternanalysis.view.ProblemPattern;
import java.util.List;

/**
 * The type Validation workflow report.
 */
public record ValidationWorkflowReport(List<ValidationResult> validationResults,
                                       List<ProblemPattern> problemPatternList) {

}
