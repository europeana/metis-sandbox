package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;


public interface  HarvestService {

    /**
     * Harvest the given file {@link MultipartFile} to a list of byte[], one string per file in the zip
     * <br/> If file is empty then an empty List will be returned
     *
     * @param file zip file containing one or more records
     * @return List of byte[]
     * @throws InvalidZipFileException  if file is not valid
     * @throws IllegalArgumentException if file does not contain any records
     */
    List<ByteArrayInputStream> harvest(MultipartFile file);

    /**
     * Harvest the given file {@link java.net.URL} one string per file in the zip
     *
     * @param URL URL for zip file containing one or more records
     * @return List of byte[]
     * @throws InvalidZipFileException  if file is not valid
     * @throws IllegalArgumentException if file does not contain any records
     */
    HttpRecordIterator harvest(String URL);
}
