package nl.recognize.dwh.application.model;

import java.util.Arrays;
import java.util.List;

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

    private String field;

    private String type;

    private boolean required;

    public Filter(List<String> operators, String queryParameter, String field, String type, boolean required) {
        this.operators = operators;
        this.queryParameter = queryParameter;
        this.field = field;
        this.type = type;
        this.required = required;
    }

    public List<String> getOperators() {
        return operators;
    }

    public String getQueryParameter() {
        return queryParameter;
    }

    public String getField() {
        return field;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }
}
