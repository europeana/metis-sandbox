package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.debias.detect.client.DeBiasClient;
import eu.europeana.metis.debias.detect.model.error.ErrorDeBiasResult;
import eu.europeana.metis.debias.detect.model.request.DetectionParameter;
import eu.europeana.metis.debias.detect.model.response.DetectionDeBiasResult;
import eu.europeana.metis.debias.detect.model.response.ValueDetection;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.EuropeanaType;
import eu.europeana.metis.schema.jibx.EuropeanaType.Choice;
import eu.europeana.metis.schema.jibx.ProxyType;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.metis.schema.jibx.ResourceOrLiteralType;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The type DeBias process service.
 */
@Service
class DeBiasProcessServiceImpl implements DeBiasProcessService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeBiasProcessServiceImpl.class);
  private final DeBiasClient deBiasClient;

  /**
   * Instantiates a new DeBias process service.
   *
   * @param deBiasClient the De Bias client
   */
  public DeBiasProcessServiceImpl(DeBiasClient deBiasClient) {
    this.deBiasClient = deBiasClient;
  }

  @Override
  public void process(List<Record> recordList) {
    Objects.requireNonNull(recordList, "List of records is required");

    DetectionParameter detectionParameter = new DetectionParameter();
    detectionParameter.setLanguage(recordList.getFirst().getLanguage().name().toLowerCase(Locale.US));
    detectionParameter.setValues(getDescriptionsFromRecordList(recordList));

    HashMap<Long, ValueDetection> report = doDeBiasAndGenerateReport(detectionParameter, recordList);
    if (!report.isEmpty()) {
      report.forEach((recordId, detect) -> {
            LOGGER.info("recordId: {} literal: {}", recordId, detect.getLiteral());
            detect.getTags().forEach(tag ->
                LOGGER.info("tag {} {} {} {}", tag.getStart(), tag.getEnd(), tag.getLength(), tag.getUri()));
          }
      );
    }
  }

  private HashMap<Long, ValueDetection> doDeBiasAndGenerateReport(DetectionParameter detectionParameter,
      List<Record> recordList) {
    HashMap<Long, ValueDetection> deBiasReport = HashMap.newHashMap(recordList.size());
    try {
      switch (deBiasClient.detect(detectionParameter)) {
        case DetectionDeBiasResult deBiasResult when deBiasResult.getDetections() != null -> {
          for (int i = 0; i < recordList.size(); i++) {
            deBiasReport.put(recordList.get(i).getRecordId(), deBiasResult.getDetections().get(i));
          }
        }
        case ErrorDeBiasResult errorDeBiasResult when errorDeBiasResult.getDetailList() != null -> {
          errorDeBiasResult.getDetailList().forEach(
              detail ->
                  LOGGER.error("{} {} {}", detail.getMsg(), detail.getType(), detail.getLoc())
          );
          deBiasReport.clear();
        }
        default -> {
          LOGGER.info("DeBias detected nothing");
          deBiasReport.clear();
        }
      }
    } catch (RuntimeException e) {
      deBiasReport.clear();
      LOGGER.error(e.getMessage(), e);
    }
    LOGGER.info("DeBias execution finished");
    return deBiasReport;
  }

  private List<String> getDescriptionsFromRecordList(List<Record> recordList) {
    return recordList
        .stream()
        .map(recordToProcess -> {
          String recordDescription = "";
          try {
            recordDescription = String.join(" ",
                getDescriptionsFromRdf(
                    new RdfConversionUtils()
                        .convertStringToRdf(
                            new String(recordToProcess.getContent(), StandardCharsets.UTF_8)
                        )
                )
            );

          } catch (SerializationException e) {
            recordDescription = "";
          }
          LOGGER.info("DeBias Execution over: {} {}", recordToProcess.getRecordId(), recordDescription);
          return recordDescription;
        }).toList();
  }

  private List<String> getDescriptionsFromRdf(RDF rdf) {
    List<ProxyType> providerProxies = this.getProviderProxies(rdf);
    List<EuropeanaType.Choice> choices = providerProxies.stream().map(EuropeanaType::getChoiceList).filter(Objects::nonNull)
                                                        .flatMap(Collection::stream).toList();

    return this.getChoicesInStringList(choices, EuropeanaType.Choice::ifDescription, EuropeanaType.Choice::getDescription,
        ResourceOrLiteralType::getString);
  }

  private boolean isProviderProxy(ProxyType proxy) {
    return proxy.getEuropeanaProxy() == null || BooleanUtils.isFalse(proxy.getEuropeanaProxy().isEuropeanaProxy());
  }

  private List<ProxyType> getProviderProxies(RDF rdf) {
    return Optional.ofNullable(rdf.getProxyList()).stream()
                   .flatMap(Collection::stream)
                   .filter(Objects::nonNull)
                   .filter(this::isProviderProxy).toList();
  }

  private <T> List<String> getChoicesInStringList(List<EuropeanaType.Choice> choices, Predicate<Choice> choicePredicate,
      Function<Choice, T> choiceGetter, Function<T, String> getString) {
    return choices.stream().filter(Objects::nonNull).filter(choicePredicate).map(choiceGetter).map(getString).toList();
  }
}
