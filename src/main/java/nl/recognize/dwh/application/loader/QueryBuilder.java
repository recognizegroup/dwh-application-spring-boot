package nl.recognize.dwh.application.loader;

import nl.recognize.dwh.application.model.Filter;

import jakarta.persistence.TypedQuery;
import java.util.UUID;

public interface QueryBuilder {
    void addPredicate(Filter baseFilter, String operator, Object value);

    TypedQuery<Object> createQuery();

    void setIdentifier(String idColumn, String identifier);
    void setIdentifier(String idColumn, UUID identifier);

    Long getCount();

    void addOrderBy(String field, boolean ascending);
}
