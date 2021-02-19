package nl.recognize.dwh.application.service;

import io.swagger.models.Path;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.info.Info;
import nl.recognize.dwh.application.loader.EntityLoader;
import nl.recognize.dwh.application.schema.EntityMapping;
import nl.recognize.dwh.application.schema.FieldMapping;
import nl.recognize.dwh.application.model.Filter;
import nl.recognize.dwh.application.schema.Mapping;
import nl.recognize.dwh.application.util.NameHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public
class DocumentationService
{
    private final String specificationVersion;
    private final String serverBaseUrl;

    private static final Map<String, String> operatorDescriptions = new HashMap<>();
    {
        operatorDescriptions.put(Filter.OPERATOR_EQUAL, "equal to");
        operatorDescriptions.put(Filter.OPERATOR_GREATER_THAN, "greater than");
        operatorDescriptions.put(Filter.OPERATOR_GREATER_OR_EQUAL_THAN, "greater than or equal to");
        operatorDescriptions.put(Filter.OPERATOR_LESS_THAN, "less than or equal to");
        operatorDescriptions.put(Filter.OPERATOR_LESS_OR_EQUAL_THAN, "less than or equal to");
    }

    @Autowired
    public DocumentationService(
            @Value("${DWH_SPECIFICATION_VERSION:1}") String specificationVersion,
            @Value("${DWH_SERVER_BASE_URL:http://localhost}") String serverBaseUrl
    ) {
        this.specificationVersion = specificationVersion;
        this.serverBaseUrl = serverBaseUrl;
    }

    public OpenAPI generate(Map<String, EntityLoader> entityTypes) {
        Paths paths = new Paths();
        Map<String, Schema> components = new HashMap<>();

        /**
         * @var string $type
         * @var EntityLoaderInterface $loader
         */
        for (Map.Entry<String, EntityLoader> entry : entityTypes.entrySet()) {
            String type = entry.getKey();
            EntityLoader loader = entry.getValue();

            List<String> names = NameHelper.splitPluralName(NameHelper.dashToCamel(type));
            if (names.size() != 2) {
                throw new IllegalStateException("Expected two plurals");
            }
            final String pluralName = names.get(0);
            final String singularName = names.get(1);
            String newName = addSchema(singularName, loader.getEntityMapping(), components);

            String pluralSchemaPath = createSchemaPath(pluralName);
            String singularSchemaPath = createSchemaPath(newName);

            addArraySchema(pluralName, singularSchemaPath, components);

            paths.addPathItem("'/" + type, createListPathItem(type, pluralSchemaPath, loader.getFilters()));
            paths.addPathItem("'/" + type + "/id", createDetailPathItem(type, singularSchemaPath, loader.getFilters()));
        }

        return new OpenAPI()
                .info(
                        new Info()
                                .version("3.0.0")
                                .description("Used for internal bridging")
                                .title("Internal API")
                )
                .components(new Components().schemas(components))
                .paths(paths)
                .servers(
                        Collections.singletonList(
                                new Server()
                                        .url(serverBaseUrl)
                        )
                );
    }

    private PathItem createListPathItem(String type, String schemaPath, List<Filter> filters) {
        List<Parameter> parameters =
            Arrays.asList(
                new Parameter()
                        .name("limit")
                        .in("query")
                        .schema(new Schema<String>().type("integer"))
                ,
                new Parameter()
                        .name("page")
                        .in("query")
                        .schema(new Schema<String>().type("integer"))
        );

        mergeFilterParametersIntoSchema(parameters, filters);

        Operation operation = new Operation()
                .parameters(parameters)
                .responses(new ApiResponses().addApiResponse("list", createResponse("List of " + type, schemaPath)));

        return new PathItem().get(operation);
    }

    private void mergeFilterParametersIntoSchema(List<Parameter> baseParameters, List<Filter> filters) {
        mergeFilterParametersIntoSchema(baseParameters, filters, false);
    }

    private void mergeFilterParametersIntoSchema(List<Parameter> parameters, List<Filter> filters, boolean softFiltersOnly) {
        for (Filter filter: filters) {
            if (!softFiltersOnly || filter.getField().isEmpty()) {
                parameters.addAll(createParametersForFilter(filter));
            }
        }
    }

    private PathItem createDetailPathItem(String type, String schemaPath, List<Filter> filters) {
        List<Parameter> parameters = Arrays.asList(
                new Parameter()
                        .name("id")
                        .in("path")
                        .required(true)
                        .schema(
                                new Schema<String>()
                                        .type("integer")
                        )
        );

        mergeFilterParametersIntoSchema(parameters, filters, true);

        Operation operation = new Operation()
                .parameters(parameters)
                .responses(new ApiResponses().addApiResponse("detail", createResponse("Detail of " + type, schemaPath)));

        return new PathItem().get(operation);
    }

    private void addArraySchema(String name, String schemaPath, Map<String, Schema> components) {
        components.put(name, createArraySchema(schemaPath));
    }

    private Schema<?> createArraySchema(String schemaPathRef) {
        return new Schema<String>().type("array").$ref(schemaPathRef);
    }

    private ApiResponse createResponse(String description, String schema) {
        return new ApiResponse()
                .description(description)
                .content(
                        new Content().addMediaType(
                            "application/json",
                            new MediaType()
                                    .schema(
                                            new Schema<String>()
                                                    .type("array")
                                                    .$ref(schema)
                                )
                        )
                );
    }

    private String addSchema(String name, EntityMapping mapping, Map<String, Schema> components) {
        Map<String, Schema> properties = new HashMap<>();

        for (FieldMapping field: mapping.getFields()) {
            String serializedName = field.getSerializedName();
            String type = field.getType();

            if (Arrays.asList(FieldMapping.TYPE_ARRAY, FieldMapping.TYPE_ENTITY).contains(type)) {
                String schemaName = StringUtils.capitalize(field.getName());

                Optional<String> arrayType = field.getArrayType();
                if (arrayType.isPresent()) {
                    properties.put(serializedName, createField(arrayType.get()));
                } else {
                    if (type.equals(FieldMapping.TYPE_ARRAY)) {
                        List<String> splitNames = NameHelper.splitPluralName(schemaName);
                        if (splitNames.size() < 2) {
                            throw new IllegalStateException("Incorrect # of strings");
                        }
                        String pluralName = splitNames.get(0);
                        schemaName = splitNames.get(1);
                    }

                    Mapping entryMapping = field.getEntryMapping();

                    String newName = schemaName;
                    boolean fieldOnly = true;
                    if (entryMapping instanceof EntityMapping) {
                        fieldOnly = false;
                        newName = addSchema(schemaName, (EntityMapping) entryMapping, components);
                    }

                    FieldMapping fieldMapping = fieldOnly ? (FieldMapping) entryMapping : null;
                    Schema schemaItem = fieldOnly
                        ?
                        createField(fieldMapping.getArrayType().isPresent() ? fieldMapping.getArrayType().get() : fieldMapping.getType())
                        :
                        new Schema().type("array").$ref(newName);

                    properties.put(serializedName, schemaItem);
                }
            } else {
                properties.put(serializedName, createField(field.getType()));
            }
        }

        if (components.containsKey(name)) {
            int appendix = 2;

            while (true) {
                String newName = name + "_" + appendix;

                if (!components.containsKey(name)) {
                    name = newName;
                    break;
                }

                appendix++;
            }
        }

        components.put(name, new Schema<String>().properties(properties));
        return name;
    }

    private Schema createField(String type) {
        if (type == null) {
            return new Schema().description("Mixed type.");
        }

        String format = null;

        if (type.equals(FieldMapping.TYPE_DATE_TIME)) {
            type = FieldMapping.TYPE_STRING;
            format = FieldMapping.TYPE_DATE_TIME;
        } else if (type.equals(FieldMapping.TYPE_EMAIL)) {
            type = FieldMapping.TYPE_STRING;
            format = FieldMapping.TYPE_EMAIL;
        }

        if (format == null) {
            return new Schema().type(type);
        } else {
            return new Schema().type(type).format(format);
        }
    }

    private List<Parameter> createParametersForFilter(Filter filter) {
        List<Parameter> result = new ArrayList<>();

        for (String operator: filter.getOperators()) {
            String name = String.format("%s[%s]", filter.getQueryParameter(), operator);

            String operatorDescription = operatorDescriptions.get(operator);

            if (operatorDescription == null) {
                throw new IllegalStateException("Unsupported operator: " + operator);
            }

            String description = String.format("Filter field %s %s where the value is of type %s", filter.getQueryParameter(), operatorDescription, filter.getType());

            if (filter.getType().equals(FieldMapping.TYPE_DATE_TIME)) {
                description += " (in format of ISO8601)";
            }

            result.add(
                    new Parameter()
                    .name(name)
                        .in("query")
                        .description(description)
                        .schema(createField(filter.getType()))
                        .required(filter.isRequired())
                    );
        }

        return result;
    }

    private String createSchemaPath(String name) {
        return String.format("#/components/schemas/%s", name);
    }
}
