package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;


public interface HarvestService {

  /**
   * Harvest the given file {@link MultipartFile} to a list of byte[], one string per file in the
   * zip <br/> If file is empty then an empty List will be returned
   *
   * @param file zip file containing one or more records
   * @return List of byte[]
   * @throws ServiceException if file is not valid, error reading file, if records are empty
   */
  List<ByteArrayInputStream> harvest(MultipartFile file) throws ServiceException;

  /**
   * Harvest the given file {@link java.net.URL} to a list of byte[], one string per file in the
   * zip
   *
   * @param url URL for zip file containing one or more records
   * @return List of byte[]
   * @throws ServiceException  if file is not valid
   */
  List<ByteArrayInputStream> harvest(String url) throws ServiceException;
}
