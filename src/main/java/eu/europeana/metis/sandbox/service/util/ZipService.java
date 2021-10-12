package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface ZipService {

  /**
   * Parse the given file {@link MultipartFile} to a list of byte[], one string per file in the zip
   * <br/> If file is empty then an empty List will be returned
   *
   * @param file zip file containing one or more files
   * @return List of byte[]
   * @throws InvalidZipFileException  if file is not valid
   * @throws IllegalArgumentException if file does not contain any records
   */
  List<ByteArrayInputStream> parse(MultipartFile file);

  List<ByteArrayInputStream> parse(String URL);
}
