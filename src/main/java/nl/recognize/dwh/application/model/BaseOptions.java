package nl.recognize.dwh.application.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
public class BaseOptions {
    private String tenant;

    private List<RequestFilter> filters;
}
