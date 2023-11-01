package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

@Service
class TransformationServiceImpl implements TransformationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationServiceImpl.class);
    private final DatasetRepository datasetRepository;
    private final TransformXsltRepository transformXsltRepository;
    private final LockRepository lockRepository;

    public TransformationServiceImpl(
            DatasetRepository datasetRepository,
            TransformXsltRepository transformXsltRepository,
            LockRepository lockRepository) {
        this.datasetRepository = datasetRepository;
        this.transformXsltRepository = transformXsltRepository;
        this.lockRepository = lockRepository;
    }

    @Override
    public RecordInfo transformToEdmInternal(Record recordToTransform) {
        requireNonNull(recordToTransform, "Record must not be null");

        final byte[] recordTransformed;
        try {
            final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = new EuropeanaIdCreator()
                    .constructEuropeanaId(recordToTransform.getContentInputStream(), recordToTransform.getDatasetId());
            XsltTransformer transformer = getTransformer(getJoinDatasetIdDatasetName(recordToTransform),
                    recordToTransform.getCountry().xmlValue(), recordToTransform.getLanguage().name().toLowerCase());
            recordTransformed = transformer
                    .transformToBytes(recordToTransform.getContent(), europeanaGeneratedIdsMap);
        } catch (TransformationException | EuropeanaIdException e) {
            throw new RecordProcessingException(recordToTransform.getProviderId(), e);
        }

        return new RecordInfo(Record.from(recordToTransform, recordTransformed));
    }

    @Override
    public RecordInfo transform(Record recordToTransform) {
        InputStream xsltContent = new ByteArrayInputStream(
                datasetRepository.getXsltContentFromDatasetId(Integer.parseInt(recordToTransform.getDatasetId()))
                        .getBytes(StandardCharsets.UTF_8));
        return new RecordInfo(Record.from(recordToTransform, transform(String.valueOf(recordToTransform.getRecordId()), xsltContent,
                recordToTransform.getContent())));
    }

    @Override
    public byte[] transform(String identifier, InputStream xsltContentInputStream,
                            byte[] recordContent) {

        final byte[] resultRecord;
        try {
            XsltTransformer transformer = getNewTransformerObject(identifier, xsltContentInputStream);
            resultRecord = transformer.transformToBytes(recordContent, null);
        } catch (TransformationException e) {
            throw new RecordProcessingException(identifier, e);
        } finally {
            closeStream(xsltContentInputStream);
        }

        return resultRecord;
    }

    private void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                LOGGER.error("Unable to close transform stream", e);
            }
        }
    }

    private XsltTransformer getTransformer(String datasetName, String edmCountry,
                                           String edmLanguage) throws TransformationException {

        if (lockRepository.isAcquired(TransformXsltRepository.LOCK_NAME_KEY)) {
            throw new TransformationException(new Exception("Default xslt is being updated"));
        }

        var xsltTransformEntity = transformXsltRepository.findById(1);
        final String xsltTransform;
        InputStream xsltInputStream = null;
        if (xsltTransformEntity.isPresent()) {
            xsltTransform = xsltTransformEntity.get().getTransformXslt();
            xsltInputStream = new ByteArrayInputStream(xsltTransform.getBytes(StandardCharsets.UTF_8));
        }

        // First argument is to be used as cacheKey, it can be any string.
        // Check implementation of constructor in metis-transformation-service module
        return new XsltTransformer("xsltKey", xsltInputStream, datasetName, edmCountry, edmLanguage);

    }

    private String getJoinDatasetIdDatasetName(Record record) {
        return getJoinDatasetIdDatasetName(record.getDatasetId(), record.getDatasetName());
    }

    private String getJoinDatasetIdDatasetName(String datasetId, String datasetName) {
        return String.join("_", datasetId, datasetName);
    }

    protected XsltTransformer getNewTransformerObject(String identifier, InputStream xsltFile)
            throws TransformationException {
        return new XsltTransformer(identifier, xsltFile);
    }
}
