package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.harvesting.FullRecord;
import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.exception.StepIsTooBigException;
import eu.europeana.metis.utils.CompressedFileExtension;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HarvestServiceImpl {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int DEFAULT_STEP_SIZE = 1;

  private final HttpHarvester httpHarvester;
  private final OaiHarvester oaiHarvester;
  private final int maxRecords;

  @Autowired
  public HarvestServiceImpl(HttpHarvester httpHarvester, OaiHarvester oaiHarvester,
      @Value("${sandbox.dataset.max-size}") int maxRecords) {
    this.httpHarvester = httpHarvester;
    this.oaiHarvester = oaiHarvester;
    this.maxRecords = maxRecords;
  }

  public List<OaiRecordHeader> harvestOaiIdentifiers(String datasetId,
      @NotNull OaiHarvest oaiHarvest, Integer stepSize) {
    try (HarvestingIterator<OaiRecordHeader, OaiRecordHeader> recordHeaderIterator = oaiHarvester.harvestRecordHeaders(
        oaiHarvest)) {
      return harvestOaiHeaders(recordHeaderIterator, datasetId, stepSize);
    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records ", e);
    }
  }

  private List<OaiRecordHeader> harvestOaiHeaders(HarvestingIterator<OaiRecordHeader,
      OaiRecordHeader> iteratorToFilter, String datasetId, Integer stepSize) throws HarvesterException {
    StopWatch watch = StopWatch.createStarted();
    final List<OaiRecordHeader> result = new ArrayList<>();
    harvestFromIterator(iteratorToFilter, datasetId, stepSize, entry -> {
      result.add(entry);

      if (watch.getTime(TimeUnit.SECONDS) > 10) {
        LOGGER.info("Already harvested {} records...", result.size());
        watch.reset();
        watch.start();
      }
      return ReportingIteration.IterationResult.CONTINUE;
    }, OaiRecordHeader::isDeleted);
    return result;
  }

  public Map<String, String> harvestFromCompressedArchive(InputStream inputStream, String datasetId, Integer stepSize,
      CompressedFileExtension compressedFileExtension) throws ServiceException {

    final List<Pair<String, Exception>> exception = new ArrayList<>(1);
    final Map<String, String> result = new HashMap<>();
    try (final HarvestingIterator<FullRecord, Path> iterator = httpHarvester.createFullRecordHarvestIterator(inputStream,
        compressedFileExtension)) {

      harvestFromIterator(iterator, datasetId, stepSize, entry -> {
        try (final InputStream content = entry.getContent()) {
          String recordId = entry.getHarvestingIdentifier();
          result.put(recordId, IOUtils.toString(content, StandardCharsets.UTF_8));
          return ReportingIteration.IterationResult.CONTINUE;

        } catch (IOException | RuntimeException e) {
          exception.add(new ImmutablePair<>(entry.getHarvestingIdentifier(), e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
      }, FullRecord::isDeleted);

      if (!exception.isEmpty()) {
        throw new HarvesterException("Could not process path " + exception.get(0).getKey() + ".",
            exception.get(0).getValue());
      }
    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting records ", e);
    }

    return result;
  }

  private <T> void harvestFromIterator(HarvestingIterator<T, ?> iterator, String datasetId,
      Integer stepSize, Function<T, ReportingIteration.IterationResult> processor,
      Predicate<T> isDeleted) throws HarvesterException {

    final int numberOfRecordsToStepInto = stepSize == null ? DEFAULT_STEP_SIZE : stepSize;
    final AtomicInteger numberOfSelectedHeaders = new AtomicInteger();
    final AtomicInteger currentIndex = new AtomicInteger();
    final AtomicInteger nextIndexToSelect = new AtomicInteger(numberOfRecordsToStepInto - 1);

    iterator.forEach(entry -> {
      if (numberOfSelectedHeaders.get() >= maxRecords) {
        //TODO: MET-4888 This method currently causes no race condition issues. But if harvesting is to ever happen
        //TODO: through multiple nodes, then a race condition will surface because of the method bellow.
        numberOfSelectedHeaders.set(maxRecords);
        return ReportingIteration.IterationResult.TERMINATE;
      }

      ReportingIteration.IterationResult result = null;
      if (currentIndex.get() == nextIndexToSelect.get()) {
        if (isDeleted.test(entry)) {
          nextIndexToSelect.getAndIncrement();
        } else {
          result = processor.apply(entry);
          nextIndexToSelect.addAndGet(numberOfRecordsToStepInto);
          numberOfSelectedHeaders.getAndIncrement();
        }
      }
      currentIndex.getAndIncrement();
      return Optional.ofNullable(result).orElse(ReportingIteration.IterationResult.CONTINUE);
    });

    if (isStepSizeBiggerThanDatasetSize(numberOfSelectedHeaders.get(), currentIndex.get(),
        nextIndexToSelect.get(), numberOfRecordsToStepInto)) {
      throw new StepIsTooBigException(currentIndex.get());
    }
  }

  private boolean isStepSizeBiggerThanDatasetSize(int datasetSize, int currentIndex, int nextIndexToSelect, int stepSize) {
    return datasetSize == 0 && currentIndex > 0 && currentIndex <= nextIndexToSelect && nextIndexToSelect < stepSize;
  }
}
