package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

/**
 * The type Harvest validation step.
 */
public class HarvestValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PROVIDER = "VALIDATION-SERVICE";
    private static final String DATASET_ID_PREFIX = "val_";
    private final RecordRepository recordRepository;
    private final DatasetService datasetService;
    private ValidationStep nextValidationStep;

    public HarvestValidationStep(DatasetService datasetService, RecordRepository recordRepository) {
        this.datasetService = datasetService;
        this.recordRepository = recordRepository;
    }

    @Override
    public void setNextValidationStep(ValidationStep nextValidationStep) {
        this.nextValidationStep = nextValidationStep;
    }

    @Override
    public ValidationResult validate(Record recordToValidate) {
        try {
            UUID uuid = UUID.randomUUID();
            final String datasetName = DATASET_ID_PREFIX + uuid;
            final String datasetId = datasetService.createEmptyDataset(datasetName,
                    recordToValidate.getCountry(),
                    recordToValidate.getLanguage(), null);
            RecordEntity recordEntity = new RecordEntity(PROVIDER, datasetId);
            Record.RecordBuilder recordToHarvest = new Record.RecordBuilder();
            recordEntity = recordRepository.save(recordEntity);
            recordToValidate = recordToHarvest
                    .providerId(PROVIDER)
                    .datasetId(datasetId)
                    .datasetName(datasetName)
                    .country(recordToValidate.getCountry())
                    .language(recordToValidate.getLanguage())
                    .content(recordToValidate.getContent())
                    .recordId(recordEntity.getId())
                    .build();
            LOGGER.error("harvesting validation step virtual dataset {}", datasetName);
            return this.nextValidationStep.validate(recordToValidate);
        } catch (Exception ex) {
            LOGGER.error("harvesting validation step fail", ex);
            return new ValidationResult("harvest", ValidationResult.Status.FAILED);
        }
    }
}
