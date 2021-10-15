package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpHarvesterImpl;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
public class HarvestServiceImpl implements HarvestService {

//    private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("http", "https", "file");

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
    public HttpRecordIterator harvest(String URL) {

        String tmpFolder = System.getProperty("java.io.tmpdir");
        HttpHarvester harvester = new HttpHarvesterImpl();
        HttpRecordIterator iterator;
        try {
            iterator = harvester.harvestRecords(URL, tmpFolder);
        } catch (HarvesterException e) {
            throw new IllegalArgumentException(e);
        }
        return iterator;
    }

}
