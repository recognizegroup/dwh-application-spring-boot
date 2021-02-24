package nl.recognize.dwh.application.model;

import java.util.List;

public class DetailOptions extends BaseOptions {
    private String identifier;

    public DetailOptions(String tenant, List<RequestFilter> filters, String identifier) {
        super(tenant, filters);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
