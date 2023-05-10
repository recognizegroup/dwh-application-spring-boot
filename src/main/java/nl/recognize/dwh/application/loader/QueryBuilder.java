package nl.recognize.dwh.application.loader;

import nl.recognize.dwh.application.model.Filter;

import javax.persistence.Query;
import java.util.UUID;

public interface QueryBuilder {
    void addPredicate(Filter baseFilter, String operator, Object value);

    Query createQuery();

    void setIdentifier(String idColumn, String identifier);
    void setIdentifier(String idColumn, UUID identifier);

    Long getCount();
}
