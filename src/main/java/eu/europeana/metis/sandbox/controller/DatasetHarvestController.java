package eu.europeana.metis.sandbox.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.exception.InvalidCompressedFileException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetIdDTO;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionService;
import eu.europeana.metis.utils.CompressedFileExtension;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for triggering a dataset processing.
 */
@RestController
@RequestMapping("/dataset/")
@Tag(name = "Dataset Harvest Controller")
public class DatasetHarvestController {

  private static final String INVALID_DATASET_NAME_MESSAGE = "dataset name can only include letters, numbers, _ or - characters";
  private static final String INVALID_STEP_SIZE_MESSAGE = "Step size must be a number higher than zero";
  private static final String INVALID_URL_MESSAGE = "The provided url is invalid. Please provide a valid url.";
  private static final String EMPTY_DATA_FILE_MESSAGE = "Data file must not be empty when provided";
  private static final String EMPTY_XSLT_FILE_MESSAGE = "Xslt file must not be empty when provided";

  private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");
  private final UrlValidator urlValidator;
  private static final Map<String, CompressedFileExtension> contentTypeToExtension = Map.of(
      "application/gzip", CompressedFileExtension.GZIP,
      "application/zip", CompressedFileExtension.ZIP,
      "application/x-tar", CompressedFileExtension.TAR
  );

  private final DatasetExecutionService datasetExecutionService;

  /**
   * Constructor.
   *
   * @param datasetExecutionService service responsible for handling dataset execution logic
   * @param urlValidator utility for validating incoming URLs
   */
  @Autowired
  public DatasetHarvestController(DatasetExecutionService datasetExecutionService, UrlValidator urlValidator) {
    this.datasetExecutionService = datasetExecutionService;
    this.urlValidator = urlValidator;
  }

  /**
   * POST API calls for harvesting and processing the records given a URL of an OAI-PMH endpoint
   *
   * @param jwtPrincipal the authenticated JWT principal containing user information
   * @param datasetName the given name of the dataset to be processed
   * @param country the given country from which the records refer to
   * @param language the given language that the records contain
   * @param stepsize the stepsize
   * @param url the given URL of the OAI-PMH repository to be processed
   * @param setspec forms a unique identifier for the set within the repository, it must be unique for eac set.
   * @param metadataformat or metadata prefix is a string to specify the metadata format in OAI-PMH requests issued to the
   * repository
   * @param xsltFile the xslt file used for transformation to edm external
   * @return 202 if it's processed correctly, 4xx or 500 otherwise
   * @throws IOException if there was something wrong with the xslt file
   */
  @Operation(summary = "Harvest dataset from OAI-PMH protocol", description = "Process the given dataset using OAI-PMH")
  @ApiResponse(responseCode = "202")
  @ApiResponse(responseCode = "400")
  @PostMapping(value = "{name}/harvestOaiPmh", produces = APPLICATION_JSON_VALUE, consumes = {
      MULTIPART_FORM_DATA_VALUE, "*/*"})
  @RequestBody(content = {@Content(mediaType = MULTIPART_FORM_DATA_VALUE)})
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDTO harvestDatasetOaiPmh(
      @AuthenticationPrincipal Jwt jwtPrincipal,
      @Parameter(description = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @Parameter(description = "country of the dataset", required = true) @RequestParam("country") Country country,
      @Parameter(description = "language of the dataset", required = true) @RequestParam("language") Language language,
      @Parameter(description = "step size to apply in record selection", schema = @Schema(description = "step size", defaultValue = "1"))
      @RequestParam(name = "stepsize", required = false, defaultValue = "1") int stepsize,
      @Parameter(description = "dataset URL records", required = true) @RequestParam("url") String url,
      @Parameter(description = "dataset specification") @RequestParam(name = "setspec", required = false) String setspec,
      @Parameter(description = "metadata format") @RequestParam("metadataformat") String metadataformat,
      @Parameter(description = "xslt file to transform to EDM external") @RequestParam(name = "xsltFile", required = false) MultipartFile xsltFile)
      throws IOException {
    //Check user id if any. This is temporarily allowed due to api and ui user security.
    final String userId;
    if (jwtPrincipal == null) {
      userId = null;
    } else {
      userId = getUserId(jwtPrincipal);
    }
    checkArgument(NAME_PATTERN.matcher(datasetName).matches(), INVALID_DATASET_NAME_MESSAGE);
    checkArgument(urlValidator.isValid(url), INVALID_URL_MESSAGE);
    checkArgument(xsltFile == null || !xsltFile.isEmpty(), EMPTY_XSLT_FILE_MESSAGE);
    checkArgument(stepsize > 0, INVALID_STEP_SIZE_MESSAGE);

    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(datasetName, country, language);
    String createdDatasetId = datasetExecutionService.createDatasetAndSubmitExecution(datasetMetadataRequest, stepsize,
        url, setspec, metadataformat, xsltFile, userId);

    return new DatasetIdDTO(createdDatasetId);
  }

  /**
   * POST API calls for harvesting and processing the records given a zip, tar or tar.gz file
   *
   * @param jwtPrincipal the authenticated user provided as a Jwt token
   * @param datasetName the given name of the dataset to be processed
   * @param country the given country from which the records refer to
   * @param language the given language that the records contain
   * @param stepsize the stepsize
   * @param datasetRecordsCompressedFile the given dataset itself to be processed as a compressed file
   * @param xsltFile the xslt file used for transformation to edm external
   * @return 202 if it's processed correctly, 4xx or 500 otherwise
   * @throws IOException if there was something wrong with the xslt file
   */
  @Operation(summary = "Harvest dataset from file", description = "Process the given dataset by HTTP providing a file")
  @ApiResponse(responseCode = "202")
  @ApiResponse(responseCode = "400")
  @PostMapping(value = "{name}/harvestByFile", produces = APPLICATION_JSON_VALUE, consumes = MULTIPART_FORM_DATA_VALUE)
  @RequestBody(content = {@Content(mediaType = MULTIPART_FORM_DATA_VALUE)})
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDTO harvestDatasetFromFile(
      @AuthenticationPrincipal Jwt jwtPrincipal,
      @Parameter(description = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @Parameter(description = "country of the dataset", required = true) @RequestParam("country") Country country,
      @Parameter(description = "language of the dataset", required = true) @RequestParam("language") Language language,
      @Parameter(description = "step size to apply in record selection", schema = @Schema(description = "step size", defaultValue = "1"))
      @RequestParam(name = "stepsize", required = false, defaultValue = "1") int stepsize,
      @Parameter(description = "dataset records uploaded in a zip, tar or tar.gz file", required = true) @RequestParam("dataset")
      MultipartFile datasetRecordsCompressedFile,
      @Parameter(description = "xslt file to transform to EDM external") @RequestParam(name = "xsltFile", required = false)
      MultipartFile xsltFile) throws IOException {
    //Check user id if any. This is temporarily allowed due to api and ui user security.
    final String userId;
    if (jwtPrincipal == null) {
      userId = null;
    } else {
      userId = getUserId(jwtPrincipal);
    }
    checkArgument(NAME_PATTERN.matcher(datasetName).matches(), INVALID_DATASET_NAME_MESSAGE);
    checkArgument(datasetRecordsCompressedFile != null && !datasetRecordsCompressedFile.isEmpty(), EMPTY_DATA_FILE_MESSAGE);
    checkArgument(xsltFile == null || !xsltFile.isEmpty(), EMPTY_XSLT_FILE_MESSAGE);
    checkArgument(stepsize > 0, INVALID_STEP_SIZE_MESSAGE);
    CompressedFileExtension compressedFileExtension = getCompressedFileExtensionTypeFromUploadedFile(
        datasetRecordsCompressedFile);

    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(datasetName, country, language);
    final String createdDatasetId = datasetExecutionService.createDatasetAndSubmitExecution(datasetMetadataRequest,
        stepsize, datasetRecordsCompressedFile, xsltFile, userId, compressedFileExtension);

    return new DatasetIdDTO(createdDatasetId);
  }

  /**
   * POST API calls for harvesting and processing the records given a URL of a compressed file
   *
   * @param jwtPrincipal the authenticated JWT principal containing user information
   * @param datasetName the given name of the dataset to be processed
   * @param country the given country from which the records refer to
   * @param language the given language that the records contain
   * @param stepsize the stepsize
   * @param url the given dataset itself to be processed as a URL of a zip file
   * @param xsltFile the xslt file used for transformation to edm external
   * @return 202 if it's processed correctly, 4xx or 500 otherwise
   */
  @Operation(summary = "Harvest dataset from url", description = "Process the given dataset by HTTP providing an URL")
  @ApiResponse(responseCode = "202")
  @ApiResponse(responseCode = "400")
  @PostMapping(value = "{name}/harvestByUrl", produces = APPLICATION_JSON_VALUE, consumes = {
      MULTIPART_FORM_DATA_VALUE, "*/*"})
  @RequestBody(content = {@Content(mediaType = MULTIPART_FORM_DATA_VALUE)})
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDTO harvestDatasetFromURL(
      @AuthenticationPrincipal Jwt jwtPrincipal,
      @Parameter(description = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @Parameter(description = "country of the dataset", required = true) @RequestParam("country") Country country,
      @Parameter(description = "language of the dataset", required = true) @RequestParam("language") Language language,
      @Parameter(description = "step size to apply in record selection", schema = @Schema(description = "step size", defaultValue = "1"))
      @RequestParam(name = "stepsize", required = false, defaultValue = "1") int stepsize,
      @Parameter(description = "dataset records URL to download in a zip file", required = true) @RequestParam("url") String url,
      @Parameter(description = "xslt file to transform to EDM external") @RequestParam(name = "xsltFile", required = false) MultipartFile xsltFile) {
    //Check user id if any. This is temporarily allowed due to api and ui user security.
    final String userId;
    if (jwtPrincipal == null) {
      userId = null;
    } else {
      userId = getUserId(jwtPrincipal);
    }
    checkArgument(NAME_PATTERN.matcher(datasetName).matches(), INVALID_DATASET_NAME_MESSAGE);
    checkArgument(urlValidator.isValid(url), INVALID_URL_MESSAGE);
    checkArgument(xsltFile == null || !xsltFile.isEmpty(), EMPTY_XSLT_FILE_MESSAGE);
    checkArgument(stepsize > 0, INVALID_STEP_SIZE_MESSAGE);
    URI uri = URI.create(url);
    CompressedFileExtension compressedFileExtension = getCompressedFileExtensionTypeFromUrl(uri);

    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(datasetName, country, language);
    final String createdDatasetId = datasetExecutionService.createDatasetAndSubmitExecution(datasetMetadataRequest,
        stepsize, url, xsltFile, userId, compressedFileExtension);

    return new DatasetIdDTO(createdDatasetId);
  }

  private CompressedFileExtension getCompressedFileExtensionTypeFromUploadedFile(MultipartFile uploadedFile) {
    String fileContentType = uploadedFile.getContentType();
    if (StringUtils.isEmpty(fileContentType)) {
      throw new InvalidCompressedFileException(new Exception("There was an issue inspecting file's content type"));
    }

    return mapContentTypeToExtension(fileContentType);
  }

  private CompressedFileExtension getCompressedFileExtensionTypeFromUrl(URI uri) {
    try {
      final String fileContentType;
      URL url = uri.toURL();
      URLConnection connection = url.openConnection();
      fileContentType = connection.getContentType();

      if (fileContentType == null || fileContentType.isBlank()) {
        throw new InvalidCompressedFileException(
            new Exception("Could not determine file's content type"));
      }

      return mapContentTypeToExtension(fileContentType);
    } catch (IOException e) {
      throw new InvalidCompressedFileException(e);
    }
  }

  private CompressedFileExtension mapContentTypeToExtension(String fileContentType) {
    return contentTypeToExtension.entrySet().stream()
                                 .filter(entry -> fileContentType.startsWith(entry.getKey()))
                                 .map(Map.Entry::getValue)
                                 .findFirst()
                                 .orElseThrow(() -> new InvalidCompressedFileException("File provided is not valid compressed file."));
  }
}
