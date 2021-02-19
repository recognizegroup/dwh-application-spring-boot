package nl.recognize.dwh.application.schema;

import lombok.Builder;
import lombok.Getter;
import nl.recognize.dwh.application.model.DataTransformation;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class EntityMapping implements Mapping
{
    private String className;

    private List<FieldMapping> fields;

    private List<DataTransformation> transformations;

    public EntityMapping addField(FieldMapping fieldMapping) {
        fields.add(fieldMapping);

        return this;
    }

    public EntityMapping addTransformation(DataTransformation transformation) {
        transformations.add(transformation);

        return this;
    }
}
