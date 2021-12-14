package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.tuple.Pair;
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
  Pair<AtomicBoolean, List<ByteArrayInputStream>> harvestZipMultipartFile(MultipartFile file) throws ServiceException;

  /**
   * Harvest the given URL {@link String} to a list of byte[], one string per file
   *
   * @param url URL for zip file containing one or more records
   * @return List of byte[]
   * @throws ServiceException if error processing URL, if URL timeout, if records are empty
   */
  Pair<AtomicBoolean, List<ByteArrayInputStream>> harvestZipUrl(String url) throws ServiceException;

  /**
   * Harvest the given OAI endpoint {@link String} to a list of byte[]
   *
   * @param endpoint for OAI endpoint containing one or more records
   * @param setSpec  record specification
   * @param prefix   record prefix
   * @return A pair with a boolean indicating if the number of harvested records reached the limit
   * and a List of byte[] with the harvested records
   * @throws ServiceException if error processing endpoint, if endpoint timeout, if records are
   *                          empty
   */
  Pair<AtomicBoolean, List<ByteArrayInputStream>> harvestOaiPmhEndpoint(String endpoint, String setSpec, String prefix)
      throws ServiceException;

}
