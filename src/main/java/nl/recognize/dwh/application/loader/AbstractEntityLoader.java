package nl.recognize.dwh.application.loader;

import lombok.RequiredArgsConstructor;
import nl.recognize.dwh.application.model.*;
import nl.recognize.dwh.application.rest.EntityNotFoundException;
import nl.recognize.dwh.application.schema.EntityMapping;
import nl.recognize.dwh.application.schema.FieldMapping;
import nl.recognize.dwh.application.schema.Mapping;
import nl.recognize.dwh.application.service.DataPipelineService;
import org.springframework.beans.factory.annotation.Value;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractEntityLoader implements EntityLoader {
    private final DataPipelineService dataPipelineService;
    private List<String> allowedOperators = Filter.OPERATORS_ALL;

    @PersistenceContext
    private EntityManager entityManager;

    private String protocolVersion;

    public AbstractEntityLoader(@Value("recognize.dwh_application.protocol_version") String protocolVersion, DataPipelineService dataPipelineService) {
        this.dataPipelineService = dataPipelineService;
        this.protocolVersion = protocolVersion;
    }

    @Transactional
    public ProtocolResponse<List<Map<String, Object>>> fetchList(ListOptions listOptions) {
        QueryBuilder queryBuilder = createQueryBuilder();

        applyFilters(queryBuilder, listOptions.getFilters());

        // TODO: naar count query
        int total = queryBuilder.createQuery()
                .getResultList()
                .size();

        List<Object> results = queryBuilder.createQuery()
                .setMaxResults(listOptions.getLimit())
                .setFirstResult((listOptions.getPage() - 1) * listOptions.getLimit())
                .getResultList();

        Map<RequestFilter, Filter> filters = getAllowedFilters(listOptions.getFilters());
        List<RequestFilter> requestFilters = new ArrayList<>(filters.keySet());

        List<Map<String, Object>> mapped = mapList(results, requestFilters);

        Map<String, String> metadata = createMetadata(listOptions, total);

        return new ProtocolResponse<>(metadata, mapped);
    }

    public ProtocolResponse<Object> fetchDetail(DetailOptions detailOptions) throws EntityNotFoundException {
        QueryBuilder queryBuilder = createQueryBuilder();

        applyIdentifier(queryBuilder, detailOptions.getIdentifier());

        try {
            Object result = queryBuilder
                    .createQuery()
                    .setMaxResults(1)
                    .getSingleResult();

            EntityMapping mapping = getEntityMapping();

            Map<String, Object> mapped = mapEntity(result, mapping, detailOptions.getFilters());
            Map<String, String> metadata = createMetadata(detailOptions);

            return new ProtocolResponse<>(metadata, mapped);
        } catch (NoResultException ex) {
            throw new EntityNotFoundException("No entity found with identifier " + detailOptions.getIdentifier());
        }
    }

    public void applyFilters(QueryBuilder queryBuilder, List<RequestFilter> filters) {
        Map<RequestFilter, Filter> allowedFilters = getAllowedFilters(filters);

        for (Map.Entry<RequestFilter, Filter> allowedFilter : allowedFilters.entrySet()) {
            String parameterName = String.format("%s_%s", allowedFilter.getValue().getField(), allowedFilter.getKey().getOperator());
            applyFilter(queryBuilder, allowedFilter.getValue(), allowedFilter.getKey());
        }
    }

    /**
     * Returns an array of tuples that contain the request filter, and the defined filter
     */
    private Map<RequestFilter, Filter> getAllowedFilters(List<RequestFilter> filters) {
        List<Filter> availableFilters = getFilters();
        Map<RequestFilter, Filter> result = new HashMap<>();

        for (RequestFilter requestFilter : filters) {
            Optional<Filter> definedFilter = availableFilters
                    .stream()
                    .filter(availableFilter -> availableFilter.getQueryParameter().equalsIgnoreCase(requestFilter.getField()))
                    .findAny();

            definedFilter.ifPresent(filter -> result.put(requestFilter, filter));
        }

        return result;
    }

    private void applyFilter(QueryBuilder queryBuilder, Filter baseFilter, RequestFilter filter) {
        // filters without a field are ignored
        if (baseFilter.getField().isEmpty()) {
            return;
        }

        if (!allowedOperators.contains(filter.getOperator())) {
            throw new IllegalStateException("Should not be possible to use this operator.");
        }

        Object value = filter.getValue();

        if (baseFilter.getType().equalsIgnoreCase(FieldMapping.TYPE_DATE_TIME)) {
            value = ZonedDateTime.parse((String) value);
        }

        queryBuilder.addPredicate(baseFilter, filter.getOperator(), value);
    }

    private List<Map<String, Object>> mapList(List<Object> results, List<RequestFilter> usedFilters) {
        EntityMapping mapping = getEntityMapping();

        return results
                .stream()
                .map(entity -> mapEntity(entity, mapping, usedFilters))
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapEntity(Object entity, EntityMapping mapping, List<RequestFilter> usedFilters) {
        Map<String, Object> result = new HashMap<>();

        for (FieldMapping field : mapping.getFields()) {
            final String serializedName = field.getSerializedName();
            result.put(serializedName, mapField(entity, field, usedFilters));
        }

        return dataPipelineService.apply(result, mapping.getTransformations());
    }

    private Object mapField(Object entity, FieldMapping field, List<RequestFilter> usedFilters) {
        String name = field.getName();
        String type = field.getType();

        // boolean hasCustomClosure = field.getOptions().containsKey("value"); TODO: wat is dat??
//        if (!hasCustomClosure &&
                if (!ClassPropertyAccessor.isReadable(entity, name)) {
            throw new IllegalStateException(String.format("Field with name %s is not readable on entity", name));
        }

        List<DataTransformation> transformations = field.getTransformations();

        Map<String, Object> unprocessed = new HashMap<>();
        unprocessed.put(name,
                // TODO: hasCustom...
//                hasCustomClosure
//                ? mapField(field.getOptions().get("value"), usedFilters)
//                :
                ClassPropertyAccessor.getValue(entity, name)
        );

        Map<String, Object> value = this.dataPipelineService.apply(unprocessed, transformations);

        if (Arrays.asList(FieldMapping.TYPE_ENTITY, FieldMapping.TYPE_ARRAY).contains(type)) {
            Optional<String> arrayType = field.getArrayType();

            if (type.equals(FieldMapping.TYPE_ARRAY) && arrayType.isPresent()) {
                return value != null ? value : Collections.emptyList();
            } else if (value == null) {
                return arrayType.isPresent() ? Collections.emptyList() : null;
            } else {

                Mapping mapping = field.getEntryMapping();

                if (!(mapping instanceof EntityMapping) && !(mapping instanceof FieldMapping)) {
                    throw new IllegalStateException(String.format("Invalid entity mapping for collection at field %s", name));
                }

                if (type.equals(FieldMapping.TYPE_ARRAY)) {
                    if (arrayType.isPresent()) {
                        return (value != null) ? (List<Object>) value : new ArrayList<>();
                    }
                    List<Object> mappedList = new ArrayList<>();
                    for (Object child : (List<Object>) value) {
                        if (mapping instanceof EntityMapping) {
                            mappedList.add(mapEntity(child, (EntityMapping) mapping, usedFilters));
                        } else {
                            mappedList.add(mapField(child, (FieldMapping) mapping, usedFilters));
                        }
                    }
                    return mappedList;

                } else {
                    return mapping instanceof EntityMapping
                                    ? mapEntity(value, (EntityMapping) mapping, usedFilters)
                                    : mapField(value, (FieldMapping) mapping, usedFilters);
                }
            }
        } else {
            return value;
        }
    }

    private QueryBuilder createQueryBuilder()  {
        QueryBuilder queryBuilder = new QueryBuilderImpl();

        applyTenant(queryBuilder);

        return queryBuilder;
    }

    private Map<String, String> createMetadata(ListOptions listOptions, int total) {
        Map<String, String> metadata = createMetadata(listOptions);
        metadata.put("page", "" + listOptions.getPage());
        metadata.put("limit", "" + listOptions.getLimit());
        metadata.put("total", "" + total);

        return metadata;
    }

    private Map<String, String> createMetadata(BaseOptions detailOptions) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("protocol", protocolVersion);

        return metadata;
    }

    private class QueryBuilderImpl implements QueryBuilder {

        private final CriteriaBuilder criteriaBuilder;
        private final CriteriaQuery<Object> criteriaQuery;
        private final Root<?> root;
        private final List<Predicate> predicates = new ArrayList<>();

        public QueryBuilderImpl(
        ) {
            this.criteriaBuilder = entityManager.getCriteriaBuilder();
            this.criteriaQuery = criteriaBuilder.createQuery();

            Class<?> usedClass;
            try {
                usedClass = Class.forName(getEntityType());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unknown class", e);
            }
            this.root = criteriaQuery.from(usedClass);
        }

        @Override
        public void addPredicate(Filter baseFilter, String operator, Object value) {
            if (baseFilter.getField().isEmpty()) {
                throw new IllegalStateException("No field present");
            }
            switch (operator) {
                case Filter.OPERATOR_EQUAL:
                    predicates.add(criteriaBuilder.equal(root.get(baseFilter.getField().get()), value));
                    break;
                case Filter.OPERATOR_GREATER_OR_EQUAL_THAN:
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(baseFilter.getField().get()), (Integer) value));
                    break;
                case Filter.OPERATOR_GREATER_THAN:
                    predicates.add(criteriaBuilder.greaterThan(root.get(baseFilter.getField().get()), (Integer) value));
                    break;
                case Filter.OPERATOR_LESS_OR_EQUAL_THAN:
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(baseFilter.getField().get()), (Integer) value));
                    break;
                case Filter.OPERATOR_LESS_THAN:
                    predicates.add(criteriaBuilder.lessThan(root.get(baseFilter.getField().get()), (Integer) value));
                    break;
                default:
                    throw new IllegalStateException("Unknown operator " + operator);
            }
        }

        @Override
        public Query createQuery() {
            return entityManager.createQuery(criteriaQuery.where(getPredicates()));
        }

        private Predicate[] getPredicates() {
            return predicates.toArray(new Predicate[0]);
        }

    }

}
