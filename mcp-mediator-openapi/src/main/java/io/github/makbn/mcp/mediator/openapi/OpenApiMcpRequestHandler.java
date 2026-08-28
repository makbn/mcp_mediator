package io.github.makbn.mcp.mediator.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpMediatorRequestHandler;
import io.github.makbn.mcp.mediator.api.McpToolAdapter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class OpenApiMcpRequestHandler implements McpMediatorRequestHandler<OpenApiMcpRequestHandler.OpenApiRequest, String> {

    private static final Logger log = LoggerFactory.getLogger(OpenApiMcpRequestHandler.class);

    private final String openApiSpecUrl;
    private final String targetServerBaseUrl;
    private final Map<String, OpenApiToolAdapter> adapterMap = new HashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private ObjectMapper mapper;

    private OpenApiMcpRequestHandler(String openApiSpecUrl, String targetServerBaseUrl) {
        this.openApiSpecUrl = openApiSpecUrl;
        this.targetServerBaseUrl = targetServerBaseUrl;
    }

    public static OpenApiMcpRequestHandler create(String openApiSpecUrl, String targetServerBaseUrl) {
        return new OpenApiMcpRequestHandler(openApiSpecUrl, targetServerBaseUrl);
    }

    public static class OpenApiRequest extends HashMap<String, Object> implements McpMediatorRequest<String> {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUuid() { return UUID.randomUUID().toString(); }
    }

    record OpenApiOperationContext(String path, PathItem.HttpMethod httpMethod, Operation operation) {}

    class OpenApiToolAdapter implements McpToolAdapter<Operation> {
        final OpenApiOperationContext context;
        final ObjectMapper mapper;

        public OpenApiToolAdapter(OpenApiOperationContext context, ObjectMapper mapper) {
            this.context = context;
            this.mapper = mapper;
        }

        @Override
        public String getMethod() {
            String opId = context.operation().getOperationId();
            if (opId != null && !opId.isBlank()) {
                return opId.replaceAll("[^a-zA-Z0-9_]", "_");
            }
            return (context.httpMethod().name() + "_" + context.path()).replaceAll("[^a-zA-Z0-9_]", "_");
        }

        @Override
        public String getAnnotations() {
            return "";
        }

        @Override
        public String getDescription() {
            String desc = context.operation().getDescription();
            if (desc == null || desc.isBlank()) {
                desc = context.operation().getSummary();
            }
            return desc != null ? desc : "Call " + getMethod();
        }

        @Override
        public String getSchema() {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            Map<String, Object> properties = new HashMap<>();
            List<String> required = new ArrayList<>();

            if (context.operation().getParameters() != null) {
                for (var param : context.operation().getParameters()) {
                    Map<String, Object> paramSchema = new HashMap<>();
                    paramSchema.put("type", param.getSchema() != null ? param.getSchema().getType() : "string");
                    if (param.getDescription() != null) {
                        paramSchema.put("description", param.getDescription());
                    }
                    properties.put(param.getName(), paramSchema);
                    if (Boolean.TRUE.equals(param.getRequired())) {
                        required.add(param.getName());
                    }
                }
            }
            schema.put("properties", properties);
            if (!required.isEmpty()) {
                schema.put("required", required);
            }

            try {
                return mapper.writeValueAsString(schema);
            } catch (JsonProcessingException e) {
                throw new McpMediatorException("Failed to generate schema", e);
            }
        }

        @Override
        public Operation getSourceTool() {
            return context.operation();
        }
    }


    @Override
    public void initialize(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof ObjectMapper) {
            this.mapper = (ObjectMapper) args[0];
        } else {
            this.mapper = new ObjectMapper();
        }

        OpenAPI openAPI = new OpenAPIV3Parser().read(openApiSpecUrl);
        if (openAPI == null) {
            throw new McpMediatorException("Failed to parse OpenAPI spec from: " + openApiSpecUrl);
        }

        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    OpenApiOperationContext context = new OpenApiOperationContext(path, httpMethod, operation);
                    OpenApiToolAdapter adapter = new OpenApiToolAdapter(context, mapper);
                    adapterMap.put(adapter.getMethod(), adapter);
                    log.info("Registered OpenAPI Tool: {}", adapter.getMethod());
                });
            });
        }
    }

    @Override
    public String getName() {
        return "openapi_mcp_server";
    }

    @Override
    public String handle(OpenApiRequest openApiRequest) throws McpMediatorException {
        
        OpenApiToolAdapter adapter = adapterMap.get(openApiRequest.getName());
        if (adapter == null) {
            throw new McpMediatorException("Unknown tool: " + openApiRequest.getName());
        }

        OpenApiOperationContext context = adapter.context;
        String resolvedPath = context.path();
        
        Map<String, String> queryParams = new HashMap<>();
        if (context.operation().getParameters() != null) {
            for (var param : context.operation().getParameters()) {
                if (openApiRequest.containsKey(param.getName())) {
                    String value = String.valueOf(openApiRequest.get(param.getName()));
                    if ("path".equals(param.getIn())) {
                        resolvedPath = resolvedPath.replace("{" + param.getName() + "}", value);
                    } else if ("query".equals(param.getIn())) {
                        queryParams.put(param.getName(), value);
                    }
                }
            }
        }

        StringBuilder urlBuilder = new StringBuilder(targetServerBaseUrl).append(resolvedPath);
        if (!queryParams.isEmpty()) {
            urlBuilder.append("?");
            queryParams.forEach((k, v) -> urlBuilder.append(k).append("=").append(v).append("&"));
            urlBuilder.setLength(urlBuilder.length() - 1);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(urlBuilder.toString()))
                .method(context.httpMethod().name(), HttpRequest.BodyPublishers.noBody());

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            throw new McpMediatorException("Failed to call OpenAPI endpoint", e);
        }
    }

    @Override
    public boolean canHandle(McpMediatorRequest<?> request) {
        if (request instanceof OpenApiRequest req) {
            return adapterMap.containsKey(req.getName());
        }
        return false;
    }

    @Override
    public Collection<Class<? extends OpenApiRequest>> getAllSupportedRequestClass() {
        return List.of(OpenApiRequest.class);
    }
}
