package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.report.DatasetLogDto;
import eu.europeana.metis.sandbox.entity.DatasetLogEntity;
import eu.europeana.metis.sandbox.repository.DatasetLogRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service is responsible for logging dataset wide errors or warning
 * (errors which could not be assigned to one record, and touch whole dataset execution)
 */
@Service
public class DatasetLogServiceImpl implements DatasetLogService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DatasetRepository datasetRepository;
    private final DatasetLogRepository datasetLogRepository;

  /**
   * Constructor for the service
   * @param datasetRepository the repository of datasets to connects to
   * @param datasetLogRepository the repository of datasets logs to connect to
   */
  public DatasetLogServiceImpl(DatasetRepository datasetRepository, DatasetLogRepository datasetLogRepository) {
        this.datasetRepository = datasetRepository;
        this.datasetLogRepository = datasetLogRepository;
    }

    @Override
    @Transactional
    public void remove(String datasetId) {
        datasetLogRepository.deleteAllByDatasetDatasetId(Integer.parseInt(datasetId));
    }

    @Override
    @Transactional
    public Void log(String datasetId, Status status, String message, Throwable exception) {
        if (status == Status.FAIL) {
            LOGGER.error(message, exception);
        } else {
            LOGGER.warn(message, exception);
        }
        DatasetLogEntity log = new DatasetLogEntity();
        log.setDataset(datasetRepository.findById(Integer.parseInt(datasetId)).orElseThrow());
        log.setMessage(message);
        log.setStackTrace(ExceptionUtils.getStackTrace(exception));
        log.setStatus(status);
        datasetLogRepository.save(log);
        return null;
    }

    @Override
    @Transactional
    public Void logException(String datasetId, Throwable exception) {
        exception = unwrapFromCompletionException(exception);
        String message = buildMessageFromExceptionChain(exception);
        message = enrichMessageForUnexpectedExceptions(exception, message);
        log(datasetId, Status.FAIL, message, exception);
        return null;
    }

    @Override
    public List<DatasetLogDto> getAllLogs(String datasetId) {
        return datasetLogRepository.findByDatasetDatasetId(Integer.parseInt(datasetId))
                .stream()
                .map(entity -> new DatasetLogDto(entity.getMessage(), entity.getStatus()))
                .toList();
    }

    private Throwable unwrapFromCompletionException(Throwable exception) {
        if (exception instanceof java.util.concurrent.CompletionException && exception.getCause() != null) {
            exception = exception.getCause();
        }
        return exception;
    }

    private static String buildMessageFromExceptionChain(Throwable exception) {
        String lastMessage = null;
        List<String> distinctMessages = new ArrayList<>();
        for (Throwable exceptionFromChain : ExceptionUtils.getThrowables(exception)) {
            String message =
                    exceptionFromChain.getMessage() != null ? exceptionFromChain.getMessage() : exceptionFromChain.getClass().getName();
            if (!message.equals(lastMessage)) {
                distinctMessages.add(message);
            }
            lastMessage = message;
        }
        return String.join(" - ", distinctMessages);
    }

    private static String enrichMessageForUnexpectedExceptions(Throwable exception, String message) {
        if (!(exception instanceof ServiceException)) {
            message = "Exception occurred while sending records to execute: " + message;
        }
        return message;
    }

}
