package nl.recognize.dwh.application.model;

import java.util.List;

public class ListOptions extends BaseOptions {
    private Integer page;

    private Integer limit;

    public ListOptions(String tenant, List<RequestFilter> filters, Integer page, Integer limit) {
        super(tenant, filters);
        this.page = page;
        this.limit = limit;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getLimit() {
        return limit;
    }
}
