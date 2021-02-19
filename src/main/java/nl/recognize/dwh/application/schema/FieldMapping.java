package nl.recognize.dwh.application.schema;

import lombok.Builder;
import lombok.Getter;
import nl.recognize.dwh.application.util.NameHelper;
import nl.recognize.dwh.application.model.DataTransformation;

import java.util.*;

@Getter
@Builder
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

    /**
     * @var array|DataTransformationInterface[]
     */
    private List<DataTransformation> transformations;

    private Mapping mapping;

    /**
     * @return string
     */
    public String getSerializedName() {
        return NameHelper.camelToSnake((String) getOption("map_to", name));
    }

    public Optional<String> getArrayType() {
        return Optional.ofNullable(( String) getOption("array_type", null));
    }

    /**
     * @return string|null
     */
    public Optional<Class<?>> getParent() {
        return Optional.ofNullable((Class<?>) getOption("parent", null));
    }

    /**
     * @return EntityMapping|FieldMapping|null
     */
    public Mapping getEntryMapping() {
//        return $this -> options['entry_mapping'] ? ? null;
        return mapping;
    }

    private Object getOption(String key, Object defaultValue) {
        if (options.containsKey(key)) {
            return key;
        }
        return defaultValue;
    }

}
