package eu.europeana.metis.sandbox.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.InvalidCompressedFileException;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.XsltProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.dto.DatasetIdDto;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.ExceptionModelDto;
import eu.europeana.metis.sandbox.dto.RecordTiersInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.service.dataset.DatasetLogService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.engine.BatchJobExecutor;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.record.RecordService;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService;
import eu.europeana.metis.sandbox.service.workflow.HarvestPublishService;
import eu.europeana.metis.utils.CompressedFileExtension;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The type Dataset controller.
 */
@RestController
@RequestMapping("/dataset/")
@Tag(name = "Dataset Controller")
class DatasetController {

    private static final String MESSAGE_OPEN_TAG_STYLE = "<span style=\"font-style: normal; font-size: 125%; font-weight: 750;\">";
    private static final String MESSAGE_BODY = " - The response body will contain an object of type"
            + " <span style=\"font-style: normal; font-size: 125%; font-weight: 750;\">";
    private static final String MESSAGE_CLOSE_TAG_STYLE = "</span>";
    private static final String MESSAGE_FOR_PROCESS_DATASET =
            MESSAGE_OPEN_TAG_STYLE + "Accepted" + MESSAGE_CLOSE_TAG_STYLE
                    + MESSAGE_BODY
                    + DatasetIdDto.SWAGGER_MODEL_NAME + MESSAGE_CLOSE_TAG_STYLE;

    private static final String MESSAGE_FOR_RETRIEVE_DATASET =
            MESSAGE_OPEN_TAG_STYLE + "OK" + MESSAGE_CLOSE_TAG_STYLE
                    + MESSAGE_BODY
                    + ProgressInfoDto.PROGRESS_SWAGGER_MODEL_NAME + MESSAGE_CLOSE_TAG_STYLE;

    public static final String MESSAGE_FOR_400_CODE =
            MESSAGE_OPEN_TAG_STYLE + "Bad Request" + MESSAGE_CLOSE_TAG_STYLE
                    + " (or any other 4xx or 5xx error status code)"
                    + MESSAGE_BODY
                    + ExceptionModelDto.SWAGGER_MODEL_NAME + MESSAGE_CLOSE_TAG_STYLE;

    private static final String MESSAGE_FOR_DATASET_VALID_NAME = "dataset name can only include letters, numbers, _ or - characters";
    private static final String MESSAGE_FOR_STEP_SIZE_VALID_VALUE = "Step size must be a number higher than zero";
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");
    private static final List<String> VALID_SCHEMES_URL = List.of("http", "https", "file");
    private static final String APPLICATION_RDF_XML = "application/rdf+xml";

    private final DatasetService datasetService;
    private final DatasetLogService datasetLogService;
    private final DatasetReportService reportService;
    private final RecordService recordService;
    private final RecordLogService recordLogService;
    private final RecordTierCalculationService recordTierCalculationService;
    private final HarvestPublishService harvestPublishService;
    private final UrlValidator urlValidator;
    private final BatchJobExecutor batchJobExecutor;

    /**
     * Instantiates a new Dataset controller.
     *
     * @param datasetService the dataset service
     * @param datasetLogService the dataset log service
     * @param reportService the report service
     * @param recordService the record service
     * @param recordLogService the record log service
     * @param recordTierCalculationService the record tier calculation service
     * @param harvestPublishService the harvest publish service
     * @param debiasStateService the debias detect service
     * @param lockRegistry the lock registry
     */
    @Autowired
    public DatasetController(DatasetService datasetService, DatasetLogService datasetLogService,
                             DatasetReportService reportService, RecordService recordService,
                             RecordLogService recordLogService, RecordTierCalculationService recordTierCalculationService,
                             HarvestPublishService harvestPublishService, BatchJobExecutor batchJobExecutor) {
        this.datasetService = datasetService;
        this.datasetLogService = datasetLogService;
        this.reportService = reportService;
        this.recordService = recordService;
        this.recordLogService = recordLogService;
        this.recordTierCalculationService = recordTierCalculationService;
        this.harvestPublishService = harvestPublishService;
      this.batchJobExecutor = batchJobExecutor;
      urlValidator = new UrlValidator(VALID_SCHEMES_URL.toArray(new String[0]));
    }

    /**
     * POST API calls for harvesting and processing the records given a zip, tar or tar.gz file
     *
     * @param jwtPrincipal the authenticated user provided as a Jwt token
     * @param datasetName the given name of the dataset to be processed
     * @param country the given country from which the records refer to
     * @param language the given language that the records contain
     * @param stepsize the stepsize
     * @param dataset the given dataset itself to be processed as a compressed file
     * @param xsltFile the xslt file used for transformation to edm external
     * @return 202 if it's processed correctly, 4xx or 500 otherwise
     */
    @Operation(summary = "Harvest dataset from file", description = "Process the given dataset by HTTP providing a file")
    @ApiResponse(responseCode = "202", description = MESSAGE_FOR_PROCESS_DATASET)
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @PostMapping(value = "{name}/harvestByFile", produces = APPLICATION_JSON_VALUE, consumes = MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = {@Content(mediaType = MULTIPART_FORM_DATA_VALUE)})
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DatasetIdDto harvestDatasetFromFile(
        @AuthenticationPrincipal Jwt jwtPrincipal,
        @Parameter(description = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
        @Parameter(description = "country of the dataset", required = true) @RequestParam("country") Country country,
        @Parameter(description = "language of the dataset", required = true) @RequestParam("language") Language language,
        @Parameter(description = "step size to apply in record selection", schema = @Schema(description = "step size", defaultValue = "1"))
        @RequestParam(name = "stepsize", required = false) Integer stepsize,
        @Parameter(description = "dataset records uploaded in a zip, tar or tar.gz file", required = true) @RequestParam("dataset") MultipartFile dataset,
        @Parameter(description = "xslt file to transform to EDM external") @RequestParam(name = "xsltFile", required = false) MultipartFile xsltFile) {
        //Check user id if any. This is temporarily allowed due to api and ui user security.
        final String userId;
        if (jwtPrincipal == null) {
            userId = null;
        } else {
            userId = getUserId(jwtPrincipal);
        }
        checkArgument(NAME_PATTERN.matcher(datasetName).matches(), MESSAGE_FOR_DATASET_VALID_NAME);
        CompressedFileExtension compressedFileExtension = getCompressedFileExtensionTypeFromUploadedFile(dataset);
        if (stepsize != null) {
            checkArgument(stepsize > 0, MESSAGE_FOR_STEP_SIZE_VALID_VALUE);
        }

        final InputStream xsltInputStream = createXsltAsInputStreamIfPresent(xsltFile);
        final String createdDatasetId = datasetService.createEmptyDataset(datasetName, userId, country,
                language, xsltInputStream);
        DatasetMetadata datasetMetadata = DatasetMetadata.builder().withDatasetId(createdDatasetId)
                .withDatasetName(datasetName).withCountry(country).withLanguage(language)
                .withStepSize(stepsize).build();
        harvestPublishService.runHarvestProvidedFileAsync(dataset, datasetMetadata, compressedFileExtension)
                .exceptionally(e -> datasetLogService.logException(createdDatasetId, e));
        return new DatasetIdDto(createdDatasetId);
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
    @ApiResponse(responseCode = "202", description = MESSAGE_FOR_PROCESS_DATASET)
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @PostMapping(value = "{name}/harvestByUrl", produces = APPLICATION_JSON_VALUE, consumes = {
            MULTIPART_FORM_DATA_VALUE, "*/*"})
    @RequestBody(content = {@Content(mediaType = MULTIPART_FORM_DATA_VALUE)})
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DatasetIdDto harvestDatasetFromURL(
        @AuthenticationPrincipal Jwt jwtPrincipal,
        @Parameter(description = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
        @Parameter(description = "country of the dataset", required = true) @RequestParam("country") Country country,
        @Parameter(description = "language of the dataset", required = true) @RequestParam("language") Language language,
        @Parameter(description = "step size to apply in record selection", schema = @Schema(description = "step size", defaultValue = "1"))
        @RequestParam(name = "stepsize", required = false) Integer stepsize,
        @Parameter(description = "dataset records URL to download in a zip file", required = true) @RequestParam("url") String url,
        @Parameter(description = "xslt file to transform to EDM external") @RequestParam(name = "xsltFile", required = false) MultipartFile xsltFile) {
        //Check user id if any. This is temporarily allowed due to api and ui user security.
        final String userId;
        if (jwtPrincipal == null) {
          userId = null;
        } else {
          userId = getUserId(jwtPrincipal);
        }
        checkArgument(NAME_PATTERN.matcher(datasetName).matches(), MESSAGE_FOR_DATASET_VALID_NAME);
        checkArgument(urlValidator.isValid(url), "The provided url is invalid. Please provide a valid url.");
        URI uri = URI.create(url);
        CompressedFileExtension compressedFileExtension = getCompressedFileExtensionTypeFromUrl(uri);
        if (stepsize != null) {
            checkArgument(stepsize > 0, MESSAGE_FOR_STEP_SIZE_VALID_VALUE);
        }

        final InputStream xsltInputStream = createXsltAsInputStreamIfPresent(xsltFile);
        final String createdDatasetId = datasetService.createEmptyDataset(datasetName, userId, country,
                language, xsltInputStream);
        DatasetMetadata datasetMetadata = DatasetMetadata.builder().withDatasetId(createdDatasetId)
                .withDatasetName(datasetName).withCountry(country).withLanguage(language)
                .withStepSize(stepsize).build();
        harvestPublishService.runHarvestHttpFileAsync(url, datasetMetadata, compressedFileExtension)
                .exceptionally(e -> datasetLogService.logException(createdDatasetId, e));
        return new DatasetIdDto(createdDatasetId);
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
     * @param setspec forms a unique identifier for the set within the repository, it must be
     * unique for eac set.
     * @param metadataformat or metadata prefix is a string to specify the metadata format in OAI-PMH
     * requests issued to the repository
     * @param xsltFile the xslt file used for transformation to edm external
     * @return 202 if it's processed correctly, 4xx or 500 otherwise
     */
    @Operation(summary = "Harvest dataset from OAI-PMH protocol", description = "Process the given dataset using OAI-PMH")
    @ApiResponse(responseCode = "202", description = MESSAGE_FOR_PROCESS_DATASET)
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @PostMapping(value = "{name}/harvestOaiPmh", produces = APPLICATION_JSON_VALUE, consumes = {
            MULTIPART_FORM_DATA_VALUE, "*/*"})
    @RequestBody(content = {@Content(mediaType = MULTIPART_FORM_DATA_VALUE)})
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DatasetIdDto harvestDatasetOaiPmh(
        @AuthenticationPrincipal Jwt jwtPrincipal,
        @Parameter(description = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
        @Parameter(description = "country of the dataset", required = true) @RequestParam("country") Country country,
        @Parameter(description = "language of the dataset", required = true) @RequestParam("language") Language language,
        @Parameter(description = "step size to apply in record selection", schema = @Schema(description = "step size", defaultValue = "1"))
        @RequestParam(name = "stepsize", required = false) Integer stepsize,
        @Parameter(description = "dataset URL records", required = true) @RequestParam("url") String url,
        @Parameter(description = "dataset specification") @RequestParam(name = "setspec", required = false) String setspec,
        @Parameter(description = "metadata format") @RequestParam("metadataformat") String metadataformat,
        @Parameter(description = "xslt file to transform to EDM external") @RequestParam(name = "xsltFile", required = false) MultipartFile xsltFile) {
        //Check user id if any. This is temporarily allowed due to api and ui user security.
        final String userId;
        if (jwtPrincipal == null) {
          userId = null;
        } else {
          userId = getUserId(jwtPrincipal);
        }
        checkArgument(NAME_PATTERN.matcher(datasetName).matches(), MESSAGE_FOR_DATASET_VALID_NAME);
        checkArgument(urlValidator.isValid(url), "The provided url is invalid. Please provide a valid url.");
        if (stepsize != null) {
            checkArgument(stepsize > 0, MESSAGE_FOR_STEP_SIZE_VALID_VALUE);
        }

        InputStream xsltInputStream = createXsltAsInputStreamIfPresent(xsltFile);
        String createdDatasetId = datasetService.createEmptyDataset(datasetName, userId, country, language,
                xsltInputStream);
        DatasetMetadata datasetMetadata = DatasetMetadata.builder().withDatasetId(createdDatasetId)
                .withDatasetName(datasetName).withCountry(country).withLanguage(language)
                .withStepSize(stepsize).build();
        setspec = getDefaultSetSpecWhenNotAvailable(setspec);

        batchJobExecutor.execute(datasetMetadata, url, setspec, metadataformat);

//        harvestPublishService.runHarvestOaiPmhAsync(datasetMetadata,
//                        new OaiHarvestData(url, setspec, metadataformat, ""))
//                .exceptionally(e -> datasetLogService.logException(createdDatasetId, e));

        return new DatasetIdDto(createdDatasetId);
    }

    private static String getDefaultSetSpecWhenNotAvailable(String setspec) {
        if (setspec != null && setspec.isEmpty()) {
            setspec = null;
        }
        return setspec;
    }

    /**
     * GET API calls to return the progress status of a given dataset id
     *
     * @param datasetId The given dataset id to look for
     * @return The report of the dataset status
     */
    @Operation(summary = "Get dataset's progress", description = "Get dataset progress information")
    @ApiResponse(responseCode = "200", description = MESSAGE_FOR_RETRIEVE_DATASET)
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @GetMapping(value = "{id}/progress", produces = APPLICATION_JSON_VALUE)
    public ProgressInfoDto getDatasetProgress(
            @Parameter(description = "id of the dataset", required = true) @PathVariable("id") String datasetId) {
        //TODO 24-02-2022: We need to update the type of info encapsulate in this object. The number of duplicated record is missing for example
//        return reportService.getReport(datasetId);
        return reportService.getProgress(datasetId);
    }

    /**
     * GET API calls to return the information about a given dataset id
     *
     * @param datasetId The given dataset id to look for
     * @return The report of the dataset status
     */
    @Operation(summary = "Get dataset information", description = "Get dataset information")
    @ApiResponse(responseCode = "200", description = MESSAGE_FOR_RETRIEVE_DATASET)
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @GetMapping(value = "{id}/info", produces = APPLICATION_JSON_VALUE)
    public DatasetInfoDto getDatasetInfo(
            @Parameter(description = "id of the dataset", required = true) @PathVariable("id") String datasetId) {
        return datasetService.getDatasetInfo(datasetId);
    }

    /**
     * GET API returns the generated tier calculation view for a stored record.
     *
     * @param datasetId the dataset id
     * @param recordId the record id
     * @return the record tier calculation view
     * @throws NoRecordFoundException if record was not found
     */
    @Operation(summary = "Computes record tier calculation", description = "Gets record tier calculation result")
    @ApiResponse(responseCode = "200", description = "Response contains the tier calculation view")
    @ApiResponse(responseCode = "404", description = "Record not found")
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @GetMapping(value = "{id}/record/compute-tier-calculation", produces = APPLICATION_JSON_VALUE)
    public RecordTierCalculationView computeRecordTierCalculation(
        @PathVariable("id") String datasetId, @RequestParam("recordId") String recordId)
            throws NoRecordFoundException {
        return recordTierCalculationService.calculateTiers(recordId, datasetId);
    }

    /**
     * GET API returns the string representation of the stored record.
     *
     * @param datasetId the dataset id
     * @param recordId the record id
     * @param step the step name
     * @return the string representation of the stored record
     * @throws NoRecordFoundException if record was not found
     */
    @Operation(summary = "Gets a record", description = "Get record string representation")
    @ApiResponse(responseCode = "200", description = "String representation of record")
    @ApiResponse(responseCode = "404", description = "Record not found")
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @GetMapping(value = "{id}/record", produces = APPLICATION_RDF_XML)
    public String getRecord(@PathVariable("id") String datasetId, @RequestParam("recordId") String recordId,
        @RequestParam(name = "step", required = false) String step) throws NoRecordFoundException {
        return recordLogService.getProviderRecordString(recordId, datasetId, getSetFromStep(step));
    }

    /**
     * GET API returns the records tiers of a given dataset.
     *
     * @param datasetId the dataset id
     * @return the records tier of a given dataset
     */
    @Operation(summary = "Gets a list of records tier", description = "Get list of records tiers")
    @ApiResponse(responseCode = "200", description = "List of records tiers")
    @ApiResponse(responseCode = "404", description = "Records not found")
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @GetMapping(value = "{id}/records-tiers", produces = APPLICATION_JSON_VALUE)
    public List<RecordTiersInfoDto> getRecordsTiers(@PathVariable("id") String datasetId) {
        return recordService.getRecordsTiersNew(datasetId);
    }

    private Set<Step> getSetFromStep(String step) {
        Set<Step> steps;
        if (step == null || step.isBlank() || "HARVEST".equals(step)) {
            steps = Set.of(Step.HARVEST_FILE, Step.HARVEST_OAI_PMH);
        } else {
            try {
                steps = Set.of(Step.valueOf(step));
            } catch (IllegalArgumentException iae) {
                throw new IllegalArgumentException(String.format("Invalid step name %s", step), iae);
            }
        }
        return steps;
    }

    /**
     * Get all available countries that can be used.
     * <p>The list is retrieved based on an internal enum</p>
     *
     * @return The list of countries available
     */
    @Operation(description = "Get data of all available countries")
    @ApiResponse(responseCode = "200", description = "List containing all available countries", content = {
            @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = CountryView.class))})
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @GetMapping(value = "countries", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<CountryView> getAllCountries() {
        return Country.getCountryListSortedByName().stream().map(CountryView::new).toList();
    }

    /**
     * Get all available languages that can be used.
     * <p>The list is retrieved based on an internal enum</p>
     *
     * @return The list of languages that are available
     */
    @Operation(description = "Get data of all available languages")
    @ApiResponse(responseCode = "200", description = "List containing all available languages", content = {
            @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = LanguageView.class))})
    @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
    @GetMapping(value = "languages", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<LanguageView> getAllLanguages() {
        return Language.getLanguageListSortedByName().stream().map(LanguageView::new).toList();
    }


    private InputStream createXsltAsInputStreamIfPresent(MultipartFile xslt) {
        if (xslt != null && !xslt.isEmpty()) {
            final String contentType = xslt.getContentType();
            if (contentType == null) {
                throw new IllegalArgumentException("Something went wrong checking file's content type.");
            } else if (!contentType.contains("xml")) {
                throw new IllegalArgumentException("The given xslt file should be a single xml file.");
            }
            try {
                return new ByteArrayInputStream(xslt.getBytes());
            } catch (IOException e) {
                throw new XsltProcessingException("Something wrong happened while processing xslt file.",
                        e);
            }
        }
        return new ByteArrayInputStream(new byte[0]);
    }

    private CompressedFileExtension getCompressedFileExtensionTypeFromUrl(URI uri) {
        try {
            final String scheme = uri.getScheme();

            if ((!"file".equalsIgnoreCase(scheme) &&
                !"http".equalsIgnoreCase(scheme) &&
                !"https".equalsIgnoreCase(scheme))) {
                throw new InvalidCompressedFileException(
                    new IllegalArgumentException("Unsupported or unsafe URL scheme: " + scheme));
            }

            final String fileContentType;
            if ("file".equalsIgnoreCase(scheme)) {
                Path path = Paths.get(uri).normalize();
                fileContentType = Files.probeContentType(path);
            } else {
                URL url = uri.toURL();
                URLConnection connection = url.openConnection();
                fileContentType = connection.getContentType();
            }

            if (fileContentType == null || fileContentType.isBlank()) {
                throw new InvalidCompressedFileException(
                    new Exception("Could not determine file's content type"));
            }

            return getCompressedFileExtensionType(fileContentType);
        } catch (IOException e) {
            throw new InvalidCompressedFileException(e);
        }
    }

        private CompressedFileExtension getCompressedFileExtensionTypeFromUploadedFile(MultipartFile uploadedFile){
            String fileContentType = uploadedFile.getContentType();
            if (StringUtils.isEmpty(fileContentType)) {
                throw new InvalidCompressedFileException(new Exception("There was an issue inspecting file's content type"));
            }

            return getCompressedFileExtensionType(fileContentType);
        }

        private CompressedFileExtension getCompressedFileExtensionType(String fileContentType){
            if (fileContentType.contains("gzip")) {
                return CompressedFileExtension.TAR_GZ;
            } else if (fileContentType.contains("zip")) {
                return CompressedFileExtension.ZIP;
            } else if (fileContentType.contains("x-tar")) {
                return CompressedFileExtension.TAR;
            } else {
                throw new InvalidCompressedFileException(new Exception("The compressed file type is invalid"));
            }
        }

        private static class CountryView {

            @JsonProperty("name")
            private final String name;
            @JsonProperty("xmlValue")
            private final String xmlValue;

            /**
             * Instantiates a new Country view.
             *
             * @param country the country
             */
            CountryView(Country country) {
                this.name = country.name();
                this.xmlValue = country.xmlValue();
            }
        }

        private static class LanguageView {

            @JsonProperty("name")
            private final String name;
            @JsonProperty("xmlValue")
            private final String xmlValue;

            /**
             * Instantiates a new Language view.
             *
             * @param language the language
             */
            LanguageView(Language language) {
                this.name = language.name();
                this.xmlValue = language.xmlValue();
            }
        }
    }
