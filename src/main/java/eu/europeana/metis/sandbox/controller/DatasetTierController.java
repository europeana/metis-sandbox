package eu.europeana.metis.sandbox.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.indexing.utils.LicenseType;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.dto.RecordTiersInfoDTO;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for getting record tiers for a dataset, computing tiers for a record and getting a particular record.
 */
@RestController
@RequestMapping("/dataset/")
@Tag(name = "Dataset Tier Controller")
public class DatasetTierController {

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;
  private final RecordTierCalculationService recordTierCalculationService;

  /**
   * Constructor.
   *
   * @param executionRecordRepository the execution record repository.
   * @param executionRecordTierContextRepository the execution record tier context repository.
   * @param recordTierCalculationService the record tier calculation service.
   */
  public DatasetTierController(ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository,
      RecordTierCalculationService recordTierCalculationService) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
    this.recordTierCalculationService = recordTierCalculationService;
  }

  /**
   * GET API returns the generated tier calculation view for a stored record.
   *
   * @param datasetId the dataset id
   * @param recordId the record id
   * @return the record tier calculation view
   * @throws NoRecordFoundException if record was not found
   */
  @Operation(summary = "Computes record tier calculation", description = "Gets record tier calculation result")
  @ApiResponse(responseCode = "200")
  @ApiResponse(responseCode = "404")
  @ApiResponse(responseCode = "400")
  @GetMapping(value = "{id}/record/compute-tier-calculation", produces = APPLICATION_JSON_VALUE)
  public RecordTierCalculationView computeRecordTierCalculation(
      @PathVariable("id") String datasetId, @RequestParam("recordId") String recordId)
      throws NoRecordFoundException {
    return recordTierCalculationService.calculateTiers(recordId, datasetId);
  }

  /**
   * GET API returns the records tiers of a given dataset.
   *
   * @param datasetId the dataset id
   * @return the records tier of a given dataset
   */
  @Operation(summary = "Gets a list of records tier", description = "Get list of records tiers")
  @ApiResponse(responseCode = "200")
  @ApiResponse(responseCode = "404")
  @ApiResponse(responseCode = "400")
  @GetMapping(value = "{id}/records-tiers", produces = APPLICATION_JSON_VALUE)
  public List<RecordTiersInfoDTO> getRecordsTiers(@PathVariable("id") String datasetId) {
    List<ExecutionRecordTierContext> executionRecordTierContext = executionRecordTierContextRepository.findByIdentifier_DatasetId(
        datasetId);

    if (executionRecordTierContext.isEmpty()) {
      throw new InvalidDatasetException(datasetId);
    }

    return executionRecordTierContext.stream()
                                     .filter(this::areAllTierValuesNotNullOrEmpty)
                                     .map(DatasetTierController::from)
                                     .toList();
  }

  /**
   * GET API returns the string representation of the stored record.
   *
   * @param datasetId the dataset id
   * @param recordId the record id
   * @param step the step name
   * @return the string representation of the stored record
   * @throws NoRecordFoundException if record was not found
   */
  @Operation(summary = "Gets a record", description = "Get record string representation")
  @ApiResponse(responseCode = "200")
  @ApiResponse(responseCode = "404")
  @ApiResponse(responseCode = "400")
  @GetMapping(value = "{id}/record", produces = APPLICATION_XML_VALUE)
  public String getRecord(@PathVariable("id") String datasetId, @RequestParam("recordId") String recordId,
      @RequestParam(name = "step", required = false) String step) throws NoRecordFoundException {
    final Set<String> executionNames;
    if (StringUtils.isBlank(step)) {
      executionNames = Set.of(FullBatchJobType.HARVEST_FILE.name(), FullBatchJobType.HARVEST_OAI.name());
    } else {
      FullBatchJobType fullBatchJobType = FullBatchJobType.valueOf(step);
      executionNames = Set.of(fullBatchJobType.name());
    }
    Set<ExecutionRecord> executionRecords = executionRecordRepository.findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionNameIn(
        datasetId, recordId, executionNames);
    return executionRecords.stream().findFirst().map(ExecutionRecord::getRecordData)
                           .orElseThrow(() -> new NoRecordFoundException(recordId));
  }

  private static RecordTiersInfoDTO from(ExecutionRecordTierContext context) {
    return RecordTiersInfoDTO.builder()
                             .recordId(context.getIdentifier().getRecordId())
                             .contentTier(MediaTier.getEnum(context.getContentTier()))
                             .contentTierBeforeLicenseCorrection(MediaTier.getEnum(context.getContentTierBeforeLicenseCorrection()))
                             .license(LicenseType.valueOf(context.getLicense()))
                             .metadataTier(MetadataTier.getEnum(context.getMetadataTier()))
                             .metadataTierLanguage(MetadataTier.getEnum(context.getMetadataTierLanguage()))
                             .metadataTierEnablingElements(MetadataTier.getEnum(context.getMetadataTierEnablingElements()))
                             .metadataTierContextualClasses(MetadataTier.getEnum(context.getMetadataTierContextualClasses()))
                             .build();
  }

  private boolean areAllTierValuesNotNullOrEmpty(ExecutionRecordTierContext executionRecordTierContext) {
    return isContentTierValid(executionRecordTierContext) &&
        isMetadataTierValid(executionRecordTierContext);
  }

  private boolean isContentTierValid(ExecutionRecordTierContext executionRecordTierContext) {
    return StringUtils.isNotBlank(executionRecordTierContext.getContentTier()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getContentTierBeforeLicenseCorrection());
  }

  private boolean isMetadataTierValid(ExecutionRecordTierContext executionRecordTierContext) {
    return StringUtils.isNotBlank(executionRecordTierContext.getMetadataTier()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getMetadataTierLanguage()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getMetadataTierEnablingElements()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getMetadataTierContextualClasses()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getLicense());
  }

}
