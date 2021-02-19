package nl.recognize.dwh.application.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequestFilter {
    private String field;

    private String operator;

    private String value;
}
