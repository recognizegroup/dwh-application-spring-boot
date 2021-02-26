package nl.recognize.dwh.application.schema;

import nl.recognize.dwh.application.util.NameHelper;
import nl.recognize.dwh.application.model.DataTransformation;

import java.util.*;

public class FieldMapping implements Mapping {
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_ENTITY = "entity";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_DATE_TIME = "date-time";
    public static final String TYPE_EMAIL = "email";

    private String name;

    private String type;

    private Map<String, String> options;

    private List<DataTransformation> transformations;

    private Mapping mapping;

    public FieldMapping(String name, String type) {
        this.name = name;
        this.type = type;
        this.options = Collections.emptyMap();
        this.transformations = Collections.emptyList();
        this.mapping = null;
    }

    public FieldMapping(String name, String type, Map<String, String> options, List<DataTransformation> transformations, Mapping mapping) {
        this.name = name;
        this.type = type;
        this.options = options;
        this.transformations = transformations;
        this.mapping = mapping;
    }

    public String getSerializedName() {
        return NameHelper.camelToSnake((String) getOption("map_to", name));
    }

    public Optional<String> getArrayType() {
        return Optional.ofNullable(( String) getOption("array_type", null));
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

    public Map<String, String> getOptions() {
        return options;
    }
}
