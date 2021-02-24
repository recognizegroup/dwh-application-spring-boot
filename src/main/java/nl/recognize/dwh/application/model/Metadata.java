package nl.recognize.dwh.application.model;

public class Metadata {

    private String protocol;
    private final Integer page;
    private final Integer limit;
    private final long total;

    public Metadata(ListOptions listOptions, Long total) {
        this.page = listOptions.getPage();
        this.limit = listOptions.getLimit();
        this.total = total != null ? total : 0;
    }

    public Metadata(DetailOptions detailOptions) {
        this.page = null;
        this.limit = null;
        this.total = 1;
    }

    public String getProtocol() {
        return protocol;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getLimit() {
        return limit;
    }

    public long getTotal() {
        return total;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocol = protocolVersion;
    }
}
