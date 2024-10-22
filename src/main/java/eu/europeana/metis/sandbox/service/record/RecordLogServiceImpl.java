package eu.europeana.metis.sandbox.service.record;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
class RecordLogServiceImpl implements RecordLogService {

    private final RecordLogRepository recordLogRepository;
    private final RecordErrorLogRepository recordErrorLogRepository;
    private final RecordRepository recordRepository;

    public RecordLogServiceImpl(RecordLogRepository recordLogRepository,
                                RecordErrorLogRepository recordErrorLogRepository,
                                RecordRepository recordRepository) {
        this.recordLogRepository = recordLogRepository;
        this.recordErrorLogRepository = recordErrorLogRepository;
        this.recordRepository = recordRepository;
    }

    @Override
    @Transactional
    public void logRecordEvent(RecordProcessEvent recordRecordProcessEvent) {
        var recordToLog = recordRecordProcessEvent.getRecord();
        var recordErrors = recordRecordProcessEvent.getRecordErrors();

        RecordEntity recordEntity = recordRepository.getReferenceById(recordToLog.getRecordId());
        var recordLogEntity = new RecordLogEntity(recordEntity, new String(
            recordRecordProcessEvent.getRecord().getContent(), StandardCharsets.UTF_8),
                recordRecordProcessEvent.getStep(), recordRecordProcessEvent.getStatus());
        var recordErrorLogEntities = recordErrors.stream()
                .map(error -> new RecordErrorLogEntity(recordEntity,
                        recordRecordProcessEvent.getStep(), recordRecordProcessEvent.getStatus(), error.getMessage(),
                        error.getStackTrace()))
                .toList();
        try {
            recordLogRepository.save(recordLogEntity);
            recordErrorLogRepository.saveAll(recordErrorLogEntities);
        } catch (RuntimeException e) {
            throw new ServiceException(
                    format("Error saving record log for record: [%s]. ", recordToLog.getProviderId()), e);
        }
    }

    @Override
    public String getProviderRecordString(String recordId, String datasetId, Set<Step> step)
            throws NoRecordFoundException {

        return getRecordLogEntities(recordId, datasetId, step)
            .stream().findFirst()
            .map(RecordLogEntity::getContent)
            .orElseThrow(
                () -> new NoRecordFoundException(
                    String.format(
                        "Record not found for recordId: %s, datasetId: %s",
                        recordId, datasetId)));
    }

    @Override
    public RecordLogEntity getRecordLogEntity(String recordId, String datasetId, Step step) {
        final RecordLogEntity recordLogEntity;
        recordLogEntity = recordLogRepository.findRecordLogByRecordIdDatasetIdAndStep(recordId, datasetId, step);
        return recordLogEntity;
    }

    @Override
    public Set<RecordLogEntity> getRecordLogEntities(String recordId, String datasetId, Set<Step> steps) {
        return recordLogRepository.findRecordLogByRecordIdDatasetIdAndStepIn(recordId, datasetId, steps);
    }

    @Override
    public RecordErrorLogEntity getRecordErrorLogEntity(String recordId, String datasetId) {
        final RecordErrorLogEntity recordErrorLogEntity;
        recordErrorLogEntity = recordErrorLogRepository.findRecordLogByRecordIdAndDatasetIdAndStep(recordId, datasetId,
                Step.MEDIA_PROCESS);
        return recordErrorLogEntity;
    }

    @Override
    @Transactional
    public void remove(String datasetId) {
        requireNonNull(datasetId, "Dataset id must not be null");
        try {
            recordErrorLogRepository.deleteByRecordIdDatasetId(datasetId);
            recordLogRepository.deleteByRecordIdDatasetId(datasetId);
        } catch (RuntimeException e) {
            throw new ServiceException(
                    format("Error removing records for dataset id: [%s]. ", datasetId), e);
        }
    }
}
