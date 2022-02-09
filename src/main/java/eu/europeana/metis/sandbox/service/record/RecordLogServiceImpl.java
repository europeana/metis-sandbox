package eu.europeana.metis.sandbox.service.record;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;

import java.util.Optional;

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
    public void logRecordEvent(Event recordEvent) {
        var record = recordEvent.getBody();
        var recordErrors = recordEvent.getRecordErrors();

        RecordEntity recordEntity = recordRepository.getOne(record.getRecordId());
        var recordLogEntity = new RecordLogEntity(recordEntity, new String(recordEvent.getBody().getContent()),
                recordEvent.getStep(), recordEvent.getStatus());
        var recordErrorLogEntities = recordErrors.stream()
                .map(error -> new RecordErrorLogEntity(recordEntity,
                        recordEvent.getStep(), recordEvent.getStatus(), error.getMessage(),
                        error.getStackTrace()))
                .collect(toList());
        try {
            recordLogRepository.save(recordLogEntity);
            recordErrorLogRepository.saveAll(recordErrorLogEntities);
        } catch (RuntimeException e) {
            throw new ServiceException(
                    format("Error saving record log for record: [%s]. ", record.getProviderId()), e);
        }
    }

    @Override
    public String getProviderRecordString(String recordId, String datasetId)
            throws NoRecordFoundException {
        return Optional.ofNullable(getRecordLogEntity(recordId, datasetId)).map(RecordLogEntity::getContent)
                .orElseThrow(() -> new NoRecordFoundException(
                        String.format("Record not found for recordId: %s, datasetId: %s", recordId, datasetId)));
    }

    @Override
    public RecordLogEntity getRecordLogEntity(String recordId, String datasetId) {
        final RecordLogEntity recordLogEntity;
        recordLogEntity = recordLogRepository.findRecordLogByRecordIdDatasetIdAndStep(recordId, datasetId, Step.MEDIA_PROCESS);
        return recordLogEntity;
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
            recordErrorLogRepository.deleteByDatasetId(datasetId);
            recordLogRepository.deleteByDatasetId(datasetId);
            recordRepository.deleteByDatasetId(datasetId);
        } catch (RuntimeException e) {
            throw new ServiceException(
                    format("Error removing records for dataset id: [%s]. ", datasetId), e);
        }
    }
}
