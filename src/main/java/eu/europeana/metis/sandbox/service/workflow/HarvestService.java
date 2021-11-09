package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;


public interface HarvestService {

  /**
   * Harvest the given file {@link MultipartFile} to a list of byte[], one string per file in the
   * zip
   *
   * @param file zip file containing one or more records
   * @return List of byte[]
   * @throws ServiceException if file is not valid, error reading file, if records are empty
   */
  List<ByteArrayInputStream> harvest(MultipartFile file) throws ServiceException;

  /**
   * Harvest the given URL {@link String} to a list of byte[], one string per file in the zip
   *
   * @param url URL for zip file containing one or more records
   * @return List of byte[]
   * @throws ServiceException if file is not valid, error reading file, if records are empty
   */
  List<ByteArrayInputStream> harvest(String url) throws ServiceException;
rvest

  /**
   * Harvest the given endpoint {@link String} to a list of byte[], one string per file in the zip
   *
   * @param endpoint    for zip file containing one or more records
   * @param setSpec     record specification
   * @param prefix      record prefix
   * @param incremental Boolean to specify incremental processing
   * @return List of byte[]
   * @throws ServiceException if file is not valid, error reading file, if records are empty
   */
  List<ByteArrayInputStream> harvest(String endpoint, String setSpec, String prefix,
      Boolean incremental) throws ServiceException;

}
