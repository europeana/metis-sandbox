package eu.europeana.metis.sandbox.batch.processor;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component("normalizationItemProcessor")
@StepScope
@Setter
public class NormalizationItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;

  private final ItemProcessorUtil itemProcessorUtil;
  private final NormalizerFactory normalizerFactory = new NormalizerFactory();

  public NormalizationItemProcessor() {
    itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
  }

  @Override
  public ThrowingFunction<SuccessExecutionRecordDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return originSuccessExecutionRecordDTO -> {
      NormalizationResult normalizationResult = normalizerFactory.getNormalizer().normalize(originSuccessExecutionRecordDTO.getRecordData());

      return originSuccessExecutionRecordDTO.toBuilderOnlyIdentifiers(targetExecutionId, getExecutionName())
                                      .recordData(normalizationResult.getNormalizedRecordInEdmXml()).build();
    };
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(originSuccessExecutionRecordDTO, targetExecutionId, getExecutionName());
  }
}
