package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.report.DatasetLogDto;
import eu.europeana.metis.sandbox.entity.DatasetLogEntity;
import eu.europeana.metis.sandbox.repository.DatasetLogRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service is responsible for logging dataset wide errors or warning
 * (errors which could not be assigned to one record, and touch whole dataset execution)
 */
@Service
public class DatasetLogServiceImpl implements DatasetLogService {

  private final Logger LOGGER = LoggerFactory.getLogger(DatasetLogServiceImpl.class);

  @Autowired
  private DatasetRepository datasetRepository;
  @Autowired
  private DatasetLogRepository datasetLogRepository;

  @Override
  @Transactional
  public Void log(String datasetId, Status status, String message, Throwable e) {
    if (status == Status.FAIL) {
      LOGGER.error(message, e);
    } else {
      LOGGER.warn(message, e);
    }
    DatasetLogEntity log = new DatasetLogEntity();
    log.setDataset(datasetRepository.findById(Integer.parseInt(datasetId)).orElseThrow());
    log.setMessage(message);
    log.setStackTrace(ExceptionUtils.getStackTrace(e));
    log.setStatus(status);
    datasetLogRepository.save(log);
    return null;
  }

  @Override
  @Transactional
  public Void logException(String datasetId, Throwable e) {
    e = unwrapFromCompletionException(e);
    String message = buildMessageFromExceptionChain(e);
    message = enrichMessageForUnexpectedExceptions(e, message);
    log(datasetId, Status.FAIL, message, e);
    return null;
  }

  @Override
  public List<DatasetLogDto> getAllLogs(int datasetId) {
    return datasetLogRepository.findByDatasetDatasetId(datasetId)
                               .stream()
                               .map(entity -> new DatasetLogDto(entity.getMessage(), entity.getStatus()))
                               .collect(Collectors.toList());
  }

  private Throwable unwrapFromCompletionException(Throwable e) {
    if (e instanceof java.util.concurrent.CompletionException && e.getCause() != null) {
      e = e.getCause();
    }
    return e;
  }

  private static String buildMessageFromExceptionChain(Throwable e) {
    String lastMessage = null;
    List<String> distinctMessages = new ArrayList<>();
    for (Throwable exceptionFormChain : ExceptionUtils.getThrowables(e)) {
      String message =
          exceptionFormChain.getMessage() != null ? exceptionFormChain.getMessage() : exceptionFormChain.getClass().getName();
      if (!message.equals(lastMessage)) {
        distinctMessages.add(message);
      }
      lastMessage = message;
    }
    return distinctMessages.stream().collect(Collectors.joining(" - "));
  }

  private static String enrichMessageForUnexpectedExceptions(Throwable e, String message) {
    if (!(e instanceof ServiceException)) {
      message = "Exception occurred while sending records to execute: "+message;
    }
    return message;
  }

}
