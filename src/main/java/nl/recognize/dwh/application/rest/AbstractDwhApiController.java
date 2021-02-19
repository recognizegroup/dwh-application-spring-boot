package nl.recognize.dwh.application.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.recognize.dwh.application.loader.EntityLoader;
import nl.recognize.dwh.application.model.*;
import nl.recognize.dwh.application.security.Role;
import nl.recognize.dwh.application.service.DocumentationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;
import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractDwhApiController {
    private static final String PAGE_PARAMETER = "page";
    private static final int PAGE_DEFAULT_VALUE = 1;
    private static final String LIMIT_PARAMETER = "limit";
    private static final int LIMIT_DEFAULT_VALUE = 25;
    private static final int LIMIT_MAX_VALUE = 50;
    private final DocumentationService documentationService;
    private final Map<String, EntityLoader> entityTypes = new HashMap<>();

    @GetMapping(path = "/{type}")
    @RolesAllowed(Role.ROLE_DWH_BRIDGE)
    public ResponseEntity<ProtocolResponse<List<Map<String, Object>>>> getList(
            @PathVariable("type") String type,
            @PathParam(PAGE_PARAMETER) Optional<Integer> pageParameter,
            @PathParam(LIMIT_PARAMETER) Optional<Integer> limitParameter,
            HttpServletRequest request
    ) {
        try {
            EntityLoader loader = getEntityLoader(type);
            List<RequestFilter> filters = buildFilters(request);

            ListOptions options = buildListOptions(pageParameter, limitParameter, filters);

            return ResponseEntity.ok(loader.fetchList(options));
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

            return ResponseEntity.ok(loader.fetchDetail(options));
        } catch (EntityNotFoundException ex) {
            log.warn("Unable to find entity", ex);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/")
    public ResponseEntity<String> definitionAction() throws JsonProcessingException {
        OpenAPI documentation = documentationService.generate(entityTypes);

        return ResponseEntity.ok(Json.mapper().writeValueAsString(documentation));
    }

    protected void registerEntityType(String type, EntityLoader loader) {
        entityTypes.put(type, loader);
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

        return ListOptions
                .builder()
                .tenant(user.getUuid())
                .page(page)
                .limit(limit)
                .filters(filters)
                .build();
    }

    private DetailOptions buildDetailOptions(
            String id,
            List<RequestFilter> filters
    ) {
        DwhUser user = getUser();

        return DetailOptions
                .builder()
                .identifier(id)
                .tenant(user.getUuid())
                .filters(filters)
                .build();
    }

    private List<RequestFilter> buildFilters(
            HttpServletRequest request
    ) {
        Map<String, String[]> parameters = request.getParameterMap();
        List<RequestFilter> filters = new ArrayList<>();

        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            String fieldName = entry.getKey();
            String[] operators = entry.getValue();
            for (String operator : operators) {
                if (Filter.OPERATORS_ALL.contains(operator)) {
                    filters.add(
                            RequestFilter
                                    .builder()
                                    .field(fieldName)
                                    .operator(operator)
                                    .value("todogerke") // value zit achter de operator, zie PHP code
                                    .build()
                    );
                }
            }

        }
        return filters;
    }

    private EntityLoader getEntityLoader(String type) throws EntityNotFoundException {
        if (entityTypes.containsKey(type)) {
            return entityTypes.get(type);
        }
        throw new EntityNotFoundException(String.format("Requested entity type %s not registered.", type));
    }

    private DwhUser getUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null && context.getAuthentication().getPrincipal() instanceof DwhUser) {
            return (DwhUser) context.getAuthentication().getPrincipal();
        }
        log.warn("No active security context for DWH");
        throw new AccessDeniedException("DWH");
    }
}
