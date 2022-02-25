package eu.europeana.metis.sandbox.common;

/**
 * Class to encapsulate the data that is used for oai-pmh harvesting
 */
public class OaiHarvestData {

    private String url;
    private String setspec;
    private String metadataformat;

    public OaiHarvestData(String url, String setspec, String metadataformat) {
        this.url = url;
        this.setspec = setspec;
        this.metadataformat = metadataformat;
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

}
