package nl.recognize.dwh.application.model;

public class RequestFilter {
    private String field;

    private String operator;

    private String value;

    public RequestFilter(String field, String operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }
}
