package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Represent each step progress in the dataset report
 */
@Schema(name = "ProgressByStep")
public record ExecutionProgressByStepDTO(
    FullBatchJobType step,
    long total,
    long success,
    long fail,
    long warn,
    @JsonInclude(Include.NON_EMPTY)
    List<ErrorInfoDTO> errors
) {

}
