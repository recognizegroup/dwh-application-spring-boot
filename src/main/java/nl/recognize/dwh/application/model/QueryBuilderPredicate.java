package nl.recognize.dwh.application.model;

public class QueryBuilderPredicate<T> {
    private final String field;
    private final T value;
    private final Operator operator;

    public QueryBuilderPredicate(String field, T value, Operator operator) {
        this.field = field;
        this.value = value;
        this.operator = operator;
    }

    public String getField() {
        return field;
    }

    public T getValue() {
        return value;
    }

    public Operator getOperator() {
        return operator;
    }

    public enum Operator {
        GREATER_THAN,
        GREATER_OR_EQUAL_THAN,
        LESS_THAN,
        LESS_OR_EQUAL_THAN,
        EQUAL;
    }
}
