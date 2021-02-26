package nl.recognize.dwh.application.model;

import java.util.List;

public class BaseOptions {

    private String tenant;
    private List<RequestFilter> filters;

    public BaseOptions(String tenant, List<RequestFilter> filters) {
        this.tenant = tenant;
        this.filters = filters;
    }

    public String getTenant() {
        return tenant;
    }

    public List<RequestFilter> getFilters() {
        return filters;
    }
}
