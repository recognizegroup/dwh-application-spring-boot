package nl.recognize.dwh.application.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder()
public class ListOptions extends BaseOptions {
    private Integer page;

    private Integer limit;

    private List<RequestFilter> filters;
}
