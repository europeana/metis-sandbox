package eu.europeana.metis.sandbox.common;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.common.exception.InvalidCompressedFileException;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Utility class for resolving types of compressed files based on their content type or file extension. This class provides
 * methods to determine the correct {@code CompressedFileExtension} for files uploaded via multipart or located at a URL. It
 * supports standard MIME types and common file extension variants.
 * <p>
 * This class is not instantiable and should be used via its static methods.
 */
@UtilityClass
public final class FileTypeResolver {

  private static final Map<String, CompressedFileExtension> contentTypeToExtension = Map.of(
      // Standard types
      "application/zip", CompressedFileExtension.ZIP,
      "application/gzip", CompressedFileExtension.GZIP,
      "application/x-tar", CompressedFileExtension.TAR,

      // Alternative Common variants
      "application/x-zip-compressed", CompressedFileExtension.ZIP,
      "application/x-gzip", CompressedFileExtension.GZIP
  );

  private static final Map<String, CompressedFileExtension> filenameExtensionMapping = Map.of(
      ".zip", CompressedFileExtension.ZIP,
      ".tar", CompressedFileExtension.TAR,
      ".gz", CompressedFileExtension.GZIP
  );

  /**
   * Determines the compressed file extension from a multipart file.
   *
   * @param uploadedFile the multipart file to process.
   * @return the inferred {@link CompressedFileExtension}.
   * @throws InvalidCompressedFileException if the content type and filename do not correspond to a known compressed file
   * extension.
   */
  public static CompressedFileExtension fromMultipart(MultipartFile uploadedFile) {
    String contentType = uploadedFile.getContentType();
    String filename = uploadedFile.getOriginalFilename();
    return resolve(contentType, filename);
  }

  /**
   * Determines the compressed file extension based on the provided URI.
   *
   * @param uri the URI of the file to process.
   * @return the inferred {@link CompressedFileExtension}.
   * @throws InvalidCompressedFileException if the content type and filename do not correspond to a known compressed file
   * extension, or if an I/O error occurs.
   */
  public static CompressedFileExtension fromUrl(URI uri) {
    try {
      URL url = uri.toURL();
      URLConnection connection = url.openConnection();
      String contentType = connection.getContentType();
      String filename = uri.getPath();
      return resolve(contentType, filename);
    } catch (IOException e) {
      throw new InvalidCompressedFileException(e);
    }
  }

  private static CompressedFileExtension resolve(String contentType, String filename) {
    return resolveFromContentType(contentType)
        .or(() -> resolveFromFileExtension(filename))
        .orElseThrow(() -> new InvalidCompressedFileException(
            format("File provided is not a valid compressed file. ContentType=%s, Filename=%s", contentType, filename)));
  }

  private static Optional<CompressedFileExtension> resolveFromContentType(String contentType) {
    Optional<CompressedFileExtension> compressedFileExtension = Optional.empty();
    if (StringUtils.isNotBlank(contentType)) {
      compressedFileExtension =
          contentTypeToExtension.entrySet().stream()
                                .filter(entry -> contentType.startsWith(entry.getKey()))
                                .map(Map.Entry::getValue)
                                .findFirst();
    }
    return compressedFileExtension;
  }

  private static Optional<CompressedFileExtension> resolveFromFileExtension(String filename) {
    Optional<CompressedFileExtension> compressedFileExtension = Optional.empty();
    if (StringUtils.isNotBlank(filename)) {
      String filenameLowerCase = filename.toLowerCase(Locale.US);
      compressedFileExtension =
          filenameExtensionMapping.entrySet().stream()
                                  .filter(entry -> filenameLowerCase.endsWith(entry.getKey()))
                                  .map(Map.Entry::getValue)
                                  .findFirst();
    }
    return compressedFileExtension;
  }
}

