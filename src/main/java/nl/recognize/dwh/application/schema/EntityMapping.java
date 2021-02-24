package nl.recognize.dwh.application.schema;

import nl.recognize.dwh.application.model.DataTransformation;

import java.util.List;

public class EntityMapping implements Mapping {
    private String className;

    private List<FieldMapping> fields;

    private List<DataTransformation> transformations;

    public EntityMapping(String className, List<FieldMapping> fields, List<DataTransformation> transformations) {
        this.className = className;
        this.fields = fields;
        this.transformations = transformations;
    }

    public String getClassName() {
        return className;
    }

    public List<FieldMapping> getFields() {
        return fields;
    }

    public List<DataTransformation> getTransformations() {
        return transformations;
    }
}
