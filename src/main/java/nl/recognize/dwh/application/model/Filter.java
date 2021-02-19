package nl.recognize.dwh.application.model;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
public class Filter {
    public static final String OPERATOR_GREATER_THAN = "gt";
    public static final String OPERATOR_GREATER_OR_EQUAL_THAN = "geq";
    public static final String OPERATOR_LESS_THAN = "lt";
    public static final String OPERATOR_LESS_OR_EQUAL_THAN = "leq";
    public static final String OPERATOR_EQUAL = "eq";

    public static final List<String> OPERATORS_ALL = Arrays.asList(
            OPERATOR_GREATER_THAN,
            OPERATOR_GREATER_OR_EQUAL_THAN,
            OPERATOR_LESS_THAN,
            OPERATOR_LESS_OR_EQUAL_THAN,
            OPERATOR_EQUAL
    );

    private List<String> operators;

    private String queryParameter;

    private Optional<String> field;

    private String type;

    private boolean required;
}
