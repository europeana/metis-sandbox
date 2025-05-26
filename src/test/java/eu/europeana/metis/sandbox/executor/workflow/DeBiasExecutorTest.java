//package eu.europeana.metis.sandbox.executor.workflow;
//
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.verify;
//
//import eu.europeana.metis.sandbox.common.Status;
//import eu.europeana.metis.sandbox.common.Step;
//import eu.europeana.metis.sandbox.common.TestUtils;
//import eu.europeana.metis.sandbox.common.locale.Country;
//import eu.europeana.metis.sandbox.common.locale.Language;
//import eu.europeana.metis.sandbox.domain.Record;
//import eu.europeana.metis.sandbox.domain.RecordInfo;
//import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
//import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessService;
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//class DeBiasExecutorTest {
//
//  @Mock
//  private DeBiasProcessService service;
//
//  @InjectMocks
//  private DeBiasExecutor consumer;
//
//  @Test
//  void debias_expectSuccess() throws IOException {
//    var fileContent = new TestUtils().readFileToBytes(
//        "record" + File.separator + "media" + File.separator + "europeana_record_with_technical_metadata.xml");
//    var recordToTest = Record.builder()
//                       .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
//                       .content(fileContent)
//                       .recordId(1L).build();
//    var recordEvent = new RecordProcessEvent(new RecordInfo(recordToTest), Step.DEBIAS, Status.PENDING);
//    doNothing().when(service).process(List.of(recordToTest));
//
//    consumer.debiasProcess(List.of(recordEvent));
//
//    verify(service).process(List.of(recordToTest));
//  }
//
//}
