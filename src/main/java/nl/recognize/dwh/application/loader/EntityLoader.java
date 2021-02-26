package nl.recognize.dwh.application.loader;

import nl.recognize.dwh.application.model.*;
import nl.recognize.dwh.application.rest.EntityNotFoundException;
import nl.recognize.dwh.application.schema.EntityMapping;

import java.util.List;
import java.util.Map;

public interface EntityLoader {
    ProtocolResponse<List<Map<String, Object>>> fetchList(ListOptions listOptions);

    ProtocolResponse<Object> fetchDetail(DetailOptions detailOptions) throws EntityNotFoundException;

    /**
     * Gets the required JAVA class for this entity
     */
    Class<?> getEntityClass();

    /**
     * Gets the required type
     */
    String getType();

    /**
     * Restricts access to resources to a specific tenant
     */
    void applyTenant(QueryBuilder queryBuilder);

    /**
     * Loads a specific entity
     */
    void applyIdentifier(QueryBuilder queryBuilder, String identifier);

    /**
     * Applies filters
     */
    void applyFilters(QueryBuilder queryBuilder, List<RequestFilter> filters);

    String getEntityType();

    List<Filter> getFilters();

    /**
     * <p>
     * Translates database result to schema. How it works:
     * - Every entity is mapped FROM the database to a JSON response
     * - Fields of an entity can be added by using a field mapping
     * - If you want to change the name that ends up in the serialization, you can use map_to
     * - Sometimes, you might want to serialize a nested entity. You can use an entity type, in combination with a
     * entry_mapping, which can be:
     * - a new entity mapping
     * - a field mapping, if you want to serialize an entire entity based as one field (ex. User -> email)
     * This does require a reference to the parent class, using parent, for validation purposes
     * - You can also serialize collections using the array type and the entry_mapping
     * - If you want to serialize an array of primitive types, you can use array_type
     */
    EntityMapping getEntityMapping();
}
