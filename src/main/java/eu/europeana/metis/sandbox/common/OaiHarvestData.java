package eu.europeana.metis.sandbox.common;

/**
 * Class to encapsulate the data that is used for oai-pmh harvesting
 */
public class OaiHarvestData {

    private final String url;
    private final String setspec;
    private final String metadataformat;
    private final String oaiIdentifier;

    public OaiHarvestData() {
        this("","","","");
    }
    public OaiHarvestData(String url, String setspec, String metadataformat, String oaiIdentifier) {
        this.url = url;
        this.setspec = setspec;
        this.metadataformat = metadataformat;
        this.oaiIdentifier = oaiIdentifier;
    }

    public String getUrl() {
        return url;
    }

    public String getSetspec() {
        return setspec;
    }

    public String getMetadataformat() {
        return metadataformat;
    }

    public String getOaiIdentifier() {
        return oaiIdentifier;
    }

}
