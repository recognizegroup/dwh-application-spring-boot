package nl.recognize.dwh.application.service;

import nl.recognize.dwh.application.loader.ClassPropertyAccessor;
import nl.recognize.dwh.application.loader.EntityLoader;
import nl.recognize.dwh.application.schema.EntityMapping;
import nl.recognize.dwh.application.schema.FieldMapping;
import nl.recognize.dwh.application.schema.Mapping;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ValidationService {
    private final DocumentationService documentationService;

    public ValidationService(DocumentationService documentationService) {
        this.documentationService = documentationService;
    }

    /**
     * @return array
     */
    public List<String> validate(List<EntityLoader> loaders) {
        List<String> errors = new ArrayList<>();

        for (EntityLoader loader : loaders) {
            validateMapping(loader.getEntityMapping(), errors);
        }

        validateDocumentation(loaders, errors);

        return errors;
    }

    private void validateMapping(EntityMapping mapping, List<String> errors) {
        try {
            Class<?> entityClass = Class.forName(mapping.getClassName());
            Object instance = entityClass.getDeclaredConstructor().newInstance();

            for (FieldMapping fieldMapping : mapping.getFields()) {
                validateField(instance, fieldMapping, errors);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot instantiate class", e);
        }
    }

    private void validateField(Object instance, FieldMapping field, List<String> errors) {
        boolean hasCustomClosure = field.getOptions().containsKey("value");

        if (!hasCustomClosure && !ClassPropertyAccessor.isReadable(instance, field.getName())) {
            errors.add(String.format("Unable to read field %s for entity %s", field.getName(), instance.getClass().getName()));
            return;
        }

        String type = field.getType();
        if (Arrays.asList(FieldMapping.TYPE_ARRAY, FieldMapping.TYPE_ENTITY).contains(type)) {
            Mapping entryMapping = field.getEntryMapping();

            if (entryMapping instanceof EntityMapping) {
                validateMapping((EntityMapping) entryMapping, errors);
            } else if (entryMapping instanceof FieldMapping) {
                Optional<Class<?>> parent = ((FieldMapping) entryMapping).getParent();
                if (parent.isEmpty()) {
                    throw new IllegalStateException("Missing parent");
                }
                try {
                    instance = parent.get().getDeclaredConstructor().newInstance();
                    validateField(instance, (FieldMapping) entryMapping, errors);
                } catch (Exception e) {
                    throw new IllegalStateException("Wrong state");
                }
            }
        }
    }

    private void validateDocumentation(List<EntityLoader> loaders, List<String> errors) {
        try {
            documentationService.generate(loaders);
        } catch (Exception exception) {
            errors.add("Could not generate documentation: " + exception.getMessage());
        }
    }
}
