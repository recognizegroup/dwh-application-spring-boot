package nl.recognize.dwh.application.loader;

import nl.recognize.dwh.application.model.Filter;

import jakarta.persistence.TypedQuery;

public interface QueryBuilder {
    void addPredicate(Filter baseFilter, String operator, Object value);

    TypedQuery<Object> createQuery();

    void setIdentifier(String idColumn, String identifier);

    Long getCount();
}
