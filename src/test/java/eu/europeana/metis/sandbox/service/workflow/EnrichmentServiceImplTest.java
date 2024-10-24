package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
import eu.europeana.enrichment.rest.client.report.Report;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;

import java.io.InputStream;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceImplTest {

    @Mock
    private EnrichmentWorker enrichmentWorker;

    @InjectMocks
    private EnrichmentServiceImpl service;

    @Test
    void enrich_expectSuccess() {
        var content = "This is the content";
        var newContent = "This is new content".getBytes();
        var testRecord = Record.builder().recordId(1L)
                .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
                .datasetName("").datasetId("1").build();
        ProcessedResult<byte[]> processedResult = new ProcessedResult<>(newContent);

        when(enrichmentWorker.process(any(InputStream.class))).thenReturn(processedResult);
        var result = service.enrich(testRecord);

        assertArrayEquals(newContent, result.getRecordValue().getContent());
    }

    @Test
    void enrich_withReport_expectFail() {
        var content = "This is the content";
        var testRecord = Record.builder().recordId(1L)
                .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
                .datasetName("").datasetId("1").build();
        Report report = Report.buildEnrichmentWarn();
        HashSet<Report> reports = new HashSet<>();
        reports.add(report);
        ProcessedResult<byte[]> processedResult = new ProcessedResult<>(content.getBytes(), reports);
        when(enrichmentWorker.process(any(InputStream.class)))
                .thenReturn(processedResult);
        var recordInfo = service.enrich(testRecord);

        assertEquals(1L, recordInfo.getRecordValue().getRecordId());
        assertEquals(1, recordInfo.getErrors().size());
    }

    @Test
    void enrich_inputNull_expectFail() {
        assertThrows(NullPointerException.class, () -> service.enrich(null));
    }

    @Test
    void enrich_withException_expectFail() {
        var content = "This is the content";
        var testRecord = Record.builder().recordId(1L)
                .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
                .datasetName("").datasetId("1").build();
        when(enrichmentWorker.process(any(InputStream.class)))
                .thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> service.enrich(testRecord));
    }

    @Test
    void enrich_withErrorTypeReport_expectFail() {
        var content = "This is the content";
        var testRecord = Record.builder().recordId(1L)
                .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
                .datasetName("").datasetId("1").build();
        Report report = Report.buildEnrichmentError();
        HashSet<Report> reports = new HashSet<>();
        reports.add(report);
        ProcessedResult<byte[]> processedResult = new ProcessedResult<>(content.getBytes(), reports);
        when(enrichmentWorker.process(any(InputStream.class)))
                .thenReturn(processedResult);
        assertThrows(RecordProcessingException.class, () -> service.enrich(testRecord));
    }

}
