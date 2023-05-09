package nl.recognize.dwh.application.schema;

import nl.recognize.dwh.application.util.NameHelper;
import nl.recognize.dwh.application.model.DataTransformation;

import java.util.*;

public class FieldMapping implements Mapping {
    public static final String TYPE_LIST = "list";
    public static final String TYPE_SET = "set";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_ENTITY = "entity";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_DATE_TIME = "date-time";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_UUID = "uuid";

    private final String name;

    private final String type;

    private final Map<String, Object> options;

    private List<DataTransformation> transformations;

    private Mapping mapping;

    public FieldMapping(String name, String type) {
        this.name = name;
        this.type = type;
        this.options = Collections.emptyMap();
        this.transformations = Collections.emptyList();
        this.mapping = null;
    }

    public FieldMapping(String name, EntityMapping mapping) {
        this.name = name;
        this.type = TYPE_ENTITY;
        this.options = new HashMap<>();
        options.put("entry_mapping", mapping);
        this.transformations = Collections.emptyList();
        this.mapping = mapping;
    }

    public FieldMapping(String name, String type, Mapping mapping) {
        this.name = name;
        this.type = type;
        this.options = new HashMap<>();
        options.put("entry_mapping", mapping);
        this.transformations = Collections.emptyList();
        this.mapping = mapping;
    }

    public FieldMapping(String type) {
        this.name = null;
        this.type = type;
        this.options = new HashMap<>();
        this.transformations = Collections.emptyList();
        this.mapping = null;
    }

    public String getSerializedName() {
        return NameHelper.camelToSnake((String) getOption("map_to", name));
    }

    public Optional<Class<?>> getParent() {
        return Optional.ofNullable((Class<?>) getOption("parent", null));
    }

    public Mapping getEntryMapping() {
        return mapping;
    }

    private Object getOption(String key, Object defaultValue) {
        if (options.containsKey(key)) {
            return key;
        }
        return defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<DataTransformation> getTransformations() {
        return transformations;
    }

    public Map<String, Object> getOptions() {
        return options;
    }
}
