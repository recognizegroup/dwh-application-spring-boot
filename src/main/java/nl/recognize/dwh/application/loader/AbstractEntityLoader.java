package nl.recognize.dwh.application.loader;

import nl.recognize.dwh.application.model.*;
import nl.recognize.dwh.application.rest.EntityNotFoundException;
import nl.recognize.dwh.application.schema.EntityMapping;
import nl.recognize.dwh.application.schema.FieldMapping;
import nl.recognize.dwh.application.schema.Mapping;
import nl.recognize.dwh.application.service.DataPipelineService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractEntityLoader implements EntityLoader {
    private final DataPipelineService dataPipelineService;
    private List<String> allowedOperators = Filter.OPERATORS_ALL;

    @PersistenceContext
    private EntityManager entityManager;

    public AbstractEntityLoader(DataPipelineService dataPipelineService) {
        this.dataPipelineService = dataPipelineService;
    }

    @Transactional
    public ProtocolResponse<List<Map<String, Object>>> fetchList(ListOptions listOptions) {
        QueryBuilder queryBuilder = createQueryBuilder();

        applyFilters(queryBuilder, listOptions.getFilters());
        applySorting(queryBuilder);

        Long total = queryBuilder.getCount();

        List<Object> results = queryBuilder.createQuery()
                .setMaxResults(listOptions.getLimit())
                .setFirstResult((listOptions.getPage() - 1) * listOptions.getLimit())
                .getResultList();

        Map<RequestFilter, Filter> filters = getAllowedFilters(listOptions.getFilters());
        List<RequestFilter> requestFilters = new ArrayList<>(filters.keySet());

        List<Map<String, Object>> mapped = mapList(results, requestFilters);

        return new ProtocolResponse<>(new Metadata(listOptions, total), mapped);
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

            return new ProtocolResponse<>(new Metadata(detailOptions), mapped);
        } catch (NoResultException ex) {
            throw new EntityNotFoundException("No entity found with identifier " + detailOptions.getIdentifier());
        }
    }

    public void applyFilters(QueryBuilder queryBuilder, List<RequestFilter> filters) {
        Map<RequestFilter, Filter> allowedFilters = getAllowedFilters(filters);

        for (Map.Entry<RequestFilter, Filter> allowedFilter : allowedFilters.entrySet()) {
            applyFilter(queryBuilder, allowedFilter.getValue(), allowedFilter.getKey());
        }
    }

    @Override
    public String getIdentifierType() {
        return FieldMapping.TYPE_INTEGER;
    }

    public void applySorting(QueryBuilder queryBuilder) {
        // no sorting by default
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

        switch (baseFilter.getType()) {
            case FieldMapping.TYPE_DATE_TIME:
                value = ZonedDateTime.parse((String) value);
                break;
            case FieldMapping.TYPE_DATE_TIME_LOCAL:
                value = LocalDateTime.parse((String) value);
                break;
            case FieldMapping.TYPE_BOOLEAN:
                value = Boolean.valueOf((String) value);
                break;
            case FieldMapping.TYPE_INTEGER:
                value = Long.valueOf((String) value);
                // no conversion
                break;
            case FieldMapping.TYPE_NUMBER:
                value = Double.valueOf((String) value);
                break;
            case FieldMapping.TYPE_STRING:
            case FieldMapping.TYPE_EMAIL:
                // no conversion
                break;
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
        if (entity == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();

        entity = dataPipelineService.apply(entity, mapping.getTransformations());

        for (FieldMapping field : mapping.getFields()) {
            final String serializedName = field.getSerializedName();
            result.put(serializedName, mapField(entity, field, usedFilters));
        }

        return result;
    }

    private Object mapField(Object entity, FieldMapping field, List<RequestFilter> usedFilters) {
        String name = field.getName();
        String type = field.getType();

        boolean hasCustomClosure = field.getOptions().containsKey("value");
        if (!hasCustomClosure && !ClassPropertyAccessor.isReadable(entity, name)) {
            throw new IllegalStateException(String.format("Field with name %s is not readable on entity", name));
        }

        List<DataTransformation> transformations = field.getTransformations();

        Object unprocessed =
                hasCustomClosure
                        ? field.getOptions().get("value")
                        :
                        ClassPropertyAccessor.getValue(entity, name);

        Object value = this.dataPipelineService.apply(unprocessed, transformations);
        if (value instanceof ZonedDateTime) {
            value = ((ZonedDateTime) value).toOffsetDateTime().toString();
        } else if (value instanceof LocalDateTime) {
            value = ((LocalDateTime) value).toString();
        }

        if (Arrays.asList(FieldMapping.TYPE_ENTITY, FieldMapping.TYPE_LIST, FieldMapping.TYPE_SET).contains(type)) {
            Mapping mapping = field.getEntryMapping();

            if (!(mapping instanceof EntityMapping) && !(mapping instanceof FieldMapping)) {
                throw new IllegalStateException(String.format("Invalid entity mapping for collection at field %s", name));
            }

            if (type.equals(FieldMapping.TYPE_LIST)) {
                if (value == null) {
                    return new ArrayList<>();
                }
                List<Object> values = (List<Object>) value;
                return values.stream()
                        .map(aValue -> mapping instanceof EntityMapping
                                ? mapEntity(aValue, (EntityMapping) mapping, usedFilters)
                                : aValue
                        ).collect(Collectors.toList());
            } else if (type.equals(FieldMapping.TYPE_SET)) {
                if (value == null) {
                    return new HashSet<>();
                }
                Set<Object> values = (Set<Object>) value;
                return values.stream()
                        .map(aValue -> mapping instanceof EntityMapping
                                ? mapEntity(aValue, (EntityMapping) mapping, usedFilters)
                                : aValue
                        ).collect(Collectors.toSet());
            } else {
                return mapping instanceof EntityMapping
                        ? mapEntity(value, (EntityMapping) mapping, usedFilters)
                        : mapField(value, (FieldMapping) mapping, usedFilters);
            }
        } else {
            return value;
        }
    }

    private QueryBuilder createQueryBuilder() {
        QueryBuilder queryBuilder = new QueryBuilderImpl();

        applyTenant(queryBuilder);

        return queryBuilder;
    }

    private class QueryBuilderImpl implements QueryBuilder {
        private final Class<?> usedClass;
        private final List<QueryBuilderPredicate<?>> predicates = new ArrayList<>();
        private final List<QueryBuilderOrder> orders = new ArrayList<>();

        public QueryBuilderImpl(
        ) {
            usedClass = getEntityClass();
        }

        @Override
        public void addPredicate(Filter baseFilter, String operator, Object value) {
            if (baseFilter.getField() == null) {
                throw new IllegalStateException("No field present");
            }
            switch (operator) {
                case Filter.OPERATOR_EQUAL:
                    predicates.add(new QueryBuilderPredicate<>(baseFilter.getField(), value, QueryBuilderPredicate.Operator.EQUAL));
                    break;
                case Filter.OPERATOR_GREATER_OR_EQUAL_THAN:
                    predicates.add(new QueryBuilderPredicate<>(baseFilter.getField(), value, QueryBuilderPredicate.Operator.GREATER_OR_EQUAL_THAN));
                    break;
                case Filter.OPERATOR_GREATER_THAN:
                    predicates.add(new QueryBuilderPredicate<>(baseFilter.getField(), value, QueryBuilderPredicate.Operator.GREATER_THAN));
                    break;
                case Filter.OPERATOR_LESS_OR_EQUAL_THAN:
                    predicates.add(new QueryBuilderPredicate<>(baseFilter.getField(), value, QueryBuilderPredicate.Operator.LESS_OR_EQUAL_THAN));
                    break;
                case Filter.OPERATOR_LESS_THAN:
                    predicates.add(new QueryBuilderPredicate<>(baseFilter.getField(), value, QueryBuilderPredicate.Operator.LESS_THAN));
                    break;
                default:
                    throw new IllegalStateException("Unknown operator " + operator);
            }
        }

        @Override
        public TypedQuery<Object> createQuery() {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object> criteriaQuery = criteriaBuilder.createQuery();
            Root<?> root = (Root<?>) criteriaQuery.from(usedClass).alias("entity");
            criteriaQuery.select(root);

            return entityManager.createQuery(
                    criteriaQuery.where(this.buildRootPredicate(criteriaBuilder, root))
                            .orderBy(this.buildOrders(criteriaBuilder, root))
            );
        }

        @Override
        public Long getCount() {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
            Root<?> root = (Root<?>) criteriaQuery.from(usedClass).alias("entity");
            criteriaQuery.select(criteriaBuilder.count(root));

            return entityManager.createQuery(
                    criteriaQuery.where(this.buildRootPredicate(criteriaBuilder, root))
            ).getSingleResult();
        }

        @Override
        public void setIdentifier(String idColumn, String identifier) {
            predicates.add(
                    new QueryBuilderPredicate<>(
                            idColumn,
                            identifier,
                            QueryBuilderPredicate.Operator.EQUAL
                    )
            );
        }

        @Override
        public void setIdentifier(String idColumn, UUID identifier) {
            predicates.add(
                    new QueryBuilderPredicate<>(
                            idColumn,
                            identifier,
                            QueryBuilderPredicate.Operator.EQUAL
                    )
            );
        }

        @Override
        public void addOrderBy(String field, boolean ascending) {
            orders.add(
                    new QueryBuilderOrder(
                            field,
                            ascending
                    )
            );
        }

        private List<Order> buildOrders(CriteriaBuilder criteriaBuilder, Root<?> root) {
            List<Order> orders = new ArrayList<>();

            for (QueryBuilderOrder order : this.orders) {
                if (order.isAscending()) {
                    orders.add(criteriaBuilder.asc(root.get(order.getField())));
                } else {
                    orders.add(criteriaBuilder.desc(root.get(order.getField())));
                }
            }

            return orders;
        }

        private Predicate buildRootPredicate(CriteriaBuilder criteriaBuilder, Root<?> root) {
            Predicate rootPredicate = criteriaBuilder.conjunction();
            for (QueryBuilderPredicate<?> predicate : predicates) {
                Object value = predicate.getValue();
                String field = predicate.getField();

                Predicate result;

                switch (predicate.getOperator()) {
                    case EQUAL:
                        result = criteriaBuilder.equal(root.get(field), value);
                        break;
                    case GREATER_OR_EQUAL_THAN:
                        if (value instanceof Integer) {
                            result = criteriaBuilder.greaterThanOrEqualTo(root.get(field), (Integer) value);
                        } else if (value instanceof Long) {
                            result = criteriaBuilder.greaterThanOrEqualTo(root.get(field), (Long) value);
                        } else if (value instanceof ZonedDateTime) {
                            result = criteriaBuilder.greaterThanOrEqualTo(root.get(field), (ZonedDateTime) value);
                        } else if (value instanceof LocalDateTime) {
                            result = criteriaBuilder.greaterThanOrEqualTo(root.get(field), (LocalDateTime) value);
                        } else {
                            result = criteriaBuilder.greaterThanOrEqualTo(root.get(field), (String) value);
                        }
                        break;
                    case GREATER_THAN:
                        if (value instanceof Integer) {
                            result = criteriaBuilder.greaterThan(root.get(field), (Integer) value);
                        } else if (value instanceof Long) {
                            result = criteriaBuilder.greaterThan(root.get(field), (Long) value);
                        } else if (value instanceof ZonedDateTime) {
                            result = criteriaBuilder.greaterThan(root.get(field), (ZonedDateTime) value);
                        } else if (value instanceof LocalDateTime) {
                            result = criteriaBuilder.greaterThan(root.get(field), (LocalDateTime) value);
                        } else {
                            result = criteriaBuilder.greaterThan(root.get(field), (String) value);
                        }
                        break;
                    case LESS_OR_EQUAL_THAN:
                        if (value instanceof Integer) {
                            result = criteriaBuilder.lessThanOrEqualTo(root.get(field), (Integer) value);
                        } else if (value instanceof Long) {
                            result = criteriaBuilder.lessThanOrEqualTo(root.get(field), (Long) value);
                        } else if (value instanceof ZonedDateTime) {
                            result = criteriaBuilder.lessThanOrEqualTo(root.get(field), (ZonedDateTime) value);
                        } else if (value instanceof LocalDateTime) {
                            result = criteriaBuilder.lessThanOrEqualTo(root.get(field), (LocalDateTime) value);
                        } else {
                            result = criteriaBuilder.lessThanOrEqualTo(root.get(field), (String) value);
                        }
                        break;
                    case LESS_THAN:
                        if (value instanceof Integer) {
                            result = criteriaBuilder.lessThan(root.get(field), (Integer) value);
                        } else if (value instanceof Long) {
                            result = criteriaBuilder.lessThan(root.get(field), (Long) value);
                        } else if (value instanceof ZonedDateTime) {
                            result = criteriaBuilder.lessThan(root.get(field), (ZonedDateTime) value);
                        } else if (value instanceof LocalDateTime) {
                            result = criteriaBuilder.lessThan(root.get(field), (LocalDateTime) value);
                        } else {
                            result = criteriaBuilder.lessThan(root.get(field), (String) value);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown operator " + predicate.getOperator());
                }

                rootPredicate = criteriaBuilder.and(rootPredicate, result);
            }

            return rootPredicate;
        }
    }
}
