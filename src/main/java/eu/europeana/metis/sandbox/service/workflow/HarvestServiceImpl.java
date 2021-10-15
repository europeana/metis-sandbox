package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpHarvesterImpl;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


@Service
public class HarvestServiceImpl implements HarvestService {


    @Override
    public List<ByteArrayInputStream> harvest(MultipartFile file) {
        List<ByteArrayInputStream> records = new ArrayList<>();
        HttpHarvester harvester = new HttpHarvesterImpl();
        try {
            harvester.harvestRecords(file.getInputStream(), CompressedFileExtension.ZIP, entry -> {
                final byte[] content = entry.getEntryContent().readAllBytes();
                records.add(new ByteArrayInputStream(content));
            });

        } catch (IOException | HarvesterException ex) {
            throw new InvalidZipFileException(ex);
        }

        if (records.isEmpty()) {
            throw new IllegalArgumentException("Provided file does not contain any records");
        }

        return records;
    }

    @Override
    public List<ByteArrayInputStream> harvest(String URL) {

        String tmpFolder = System.getProperty("java.io.tmpdir");
        List<ByteArrayInputStream> records = new ArrayList<>();
        HttpHarvester harvester = new HttpHarvesterImpl();
        try {
            HttpRecordIterator iterator = harvester.harvestRecords(URL, tmpFolder);
            iterator.forEach(path -> {
                try (InputStream content = Files.newInputStream(path)) {
                    records.add(new ByteArrayInputStream(content.readAllBytes()));
                    return ReportingIteration.IterationResult.CONTINUE;
                } catch (IOException | RuntimeException e) {
                    return ReportingIteration.IterationResult.TERMINATE;
                }
            });
        } catch (HarvesterException e) {
            throw new IllegalArgumentException(e);
        }
        return records;
    }

}
