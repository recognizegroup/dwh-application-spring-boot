package nl.recognize.dwh.application.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import nl.recognize.dwh.application.loader.EntityLoader;
import nl.recognize.dwh.application.model.*;
import nl.recognize.dwh.application.security.Role;
import nl.recognize.dwh.application.service.DocumentationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/dwh")
public class DwhApiController {

    private static final Logger log = LoggerFactory.getLogger(DwhApiController.class);

    private static final String PAGE_PARAMETER = "page";
    private static final int PAGE_DEFAULT_VALUE = 1;
    private static final String LIMIT_PARAMETER = "limit";
    private static final int LIMIT_DEFAULT_VALUE = 25;
    private static final int LIMIT_MAX_VALUE = 50;

    private final String protocolVersion;
    private final DocumentationService documentationService;

    private final List<EntityLoader> entityLoaders;

    public DwhApiController(
            @Value("${nl.recognize.dwh.application.protocol.version:1.0.0}") String protocolVersion,
            DocumentationService documentationService,
            List<EntityLoader> entityLoaders
    ) {
        this.protocolVersion = protocolVersion;
        this.documentationService = documentationService;
        this.entityLoaders = entityLoaders;
    }

    @GetMapping(path = "/{type}")
    @RolesAllowed(Role.ROLE_DWH_BRIDGE)
    public ResponseEntity<ProtocolResponse<List<Map<String, Object>>>> getList(
            @PathVariable("type") String type,
            @RequestParam(PAGE_PARAMETER) Optional<Integer> pageParameter,
            @RequestParam(LIMIT_PARAMETER) Optional<Integer> limitParameter,
            HttpServletRequest request
    ) {
        try {
            EntityLoader loader = getEntityLoader(type);
            List<RequestFilter> filters = buildFilters(request);

            ListOptions options = buildListOptions(pageParameter, limitParameter, filters);

            return ResponseEntity.ok(fetchList(loader, options));
        } catch (EntityNotFoundException ex) {
            log.warn("Unable to find entity", ex);
            return ResponseEntity.notFound().build();
        } catch (InvalidParametersException ex) {
            log.warn("Invalid parameter supplied", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(path = "/{type}/{id}")
    @RolesAllowed(Role.ROLE_DWH_BRIDGE)
    public Object getDetails(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            HttpServletRequest request
    ) {
        try {
            EntityLoader loader = getEntityLoader(type);

            List<RequestFilter> filters = buildFilters(request);
            DetailOptions options = buildDetailOptions(id, filters);

            return ResponseEntity.ok(fetchDetails(loader, options));
        } catch (EntityNotFoundException ex) {
            log.warn("Unable to find entity", ex);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<String> definitionAction() throws JsonProcessingException {
        OpenAPI documentation = documentationService.generate(entityLoaders);

        return ResponseEntity.ok(Json.mapper().writeValueAsString(documentation));
    }

    private ListOptions buildListOptions(
            Optional<Integer> pageOptional,
            Optional<Integer> limitOptional,
            List<RequestFilter> filters
    ) throws InvalidParametersException {
        int page = pageOptional.orElse(PAGE_DEFAULT_VALUE);
        int limit = limitOptional.orElse(LIMIT_DEFAULT_VALUE);
        if (page <= 0 || limit > LIMIT_MAX_VALUE || limit < 0) {
            throw new InvalidParametersException();
        }

        DwhUser user = getUser();

        return new ListOptions(user.getUuid(), filters, page, limit);
    }

    private DetailOptions buildDetailOptions(
            String id,
            List<RequestFilter> filters
    ) {
        DwhUser user = getUser();

        return new DetailOptions(user.getUuid(), filters, id);
    }

    private List<RequestFilter> buildFilters(
            HttpServletRequest request
    ) {
        Map<String, String[]> parameters = request.getParameterMap();
        List<RequestFilter> filters = new ArrayList<>();

        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            for (String urlEncodedValue : entry.getValue()) {

                RequestFilter filter = parseFilter(entry.getKey(), urlEncodedValue);
                if (filter != null) {
                    filters.add(filter);
                }
            }
        }
        return filters;
    }

    /** Format for supported filters: <key>[<gt>]=<value>
     *
     * Example: createdAt[gt]=2020-01-12T12:34:12.000Z
     * @param fieldNameWithParametersUrlEncoded: part before the =
     * @param urlEncodedValue: part after the =
     */
    private RequestFilter parseFilter(String fieldNameWithParametersUrlEncoded, String urlEncodedValue) {
        String[] tokens = URLDecoder.decode(fieldNameWithParametersUrlEncoded, Charset.defaultCharset())
                .split("[\\[\\]]");

        if (tokens.length == 2) {
            String value = urlEncodedValue;
            log.debug("Created filter: name '{}', operator '{}', value '{}'", tokens[0], tokens[1], value);
            return new RequestFilter(tokens[0], tokens[1], value);
        } else if (tokens.length == 3) {
            // format was: filtername[operator]=value
            String value = tokens[2];
            if (value.startsWith("=")) {
                value = value.substring(1);
            }
            log.debug("Created filter: name '{}', operator '{}', value '{}'", tokens[0], tokens[1], value);
            return new RequestFilter(tokens[0], tokens[1], value);
        } else {
            log.debug("Skipping filter: '{}'", fieldNameWithParametersUrlEncoded);
            // unsupported filter, it does not have an operator. Note: do not fail, it might be
            // the optional page / limit parameter, which are handled as @RequestParam
            return null;
        }
    }

    private EntityLoader getEntityLoader(String type) throws EntityNotFoundException {
        return entityLoaders
                .stream()
                .filter(entityLoader -> entityLoader.getType().equals(type))
                .findAny().orElseThrow(() -> new EntityNotFoundException(String.format("Requested entity type %s not registered.", type)));
    }

    private DwhUser getUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null && context.getAuthentication().getPrincipal() instanceof DwhUser) {
            return (DwhUser) context.getAuthentication().getPrincipal();
        }
        log.warn("No active security context for DWH");
        throw new AccessDeniedException("DWH");
    }

    private ProtocolResponse<List<Map<String, Object>>> fetchList(EntityLoader loader, ListOptions options) {
        ProtocolResponse<List<Map<String, Object>>> response = loader.fetchList(options);
        response.getMetadata().setProtocolVersion(protocolVersion);
        return response;
    }

    private ProtocolResponse<Object> fetchDetails(EntityLoader loader, DetailOptions options) throws
            EntityNotFoundException {
        ProtocolResponse<Object> response = loader.fetchDetail(options);
        response.getMetadata().setProtocolVersion(protocolVersion);
        return response;
    }
}
