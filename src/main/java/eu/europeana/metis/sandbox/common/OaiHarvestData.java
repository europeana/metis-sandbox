package eu.europeana.metis.sandbox.common;

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

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSetspec() {
        return setspec;
    }

    public void setSetspec(String setspec) {
        this.setspec = setspec;
    }

    public String getMetadataformat() {
        return metadataformat;
    }

    public void setMetadataformat(String metadataformat) {
        this.metadataformat = metadataformat;
    }
}
