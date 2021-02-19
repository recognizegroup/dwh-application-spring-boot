package nl.recognize.dwh.application.loader;

import nl.recognize.dwh.application.model.Filter;

import javax.persistence.Query;

public interface QueryBuilder {
    void addPredicate(Filter baseFilter, String operator, Object value);

    Query createQuery();
}
