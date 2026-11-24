package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.core.internal.McpMethodArgumentResolver;
import io.github.makbn.mcp.mediator.core.internal.McpMethodSchemaGenerator;
import io.github.makbn.mcp.mediator.core.util.McpAnnotations;
import io.github.makbn.mcp.mediator.core.util.McpUtils;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.modern.McpSchema;
import io.modelcontextprotocol.modern.server.McpRequestContext;
import io.modelcontextprotocol.modern.server.McpServer;
import io.modelcontextprotocol.modern.server.McpSyncResponse;
import io.modelcontextprotocol.modern.server.feature.*;
import io.modelcontextprotocol.modern.server.transport.HttpServletMcpTransport;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Serves annotated beans through the MCP SDK's "modern" server API (the newer stateless-request protocol that Spring AI
 * 2.1 supports with its {@code Modern} providers). Same annotations (own or Spring AI's) as the classic server;
 * tools, resources, resource templates, prompts and completions are supported. Parameters of type
 * {@link McpRequestContext}, Spring AI's {@code McpModernSyncRequestContext} and {@code McpMeta} are injected.
 */
public final class McpModernServer {
    private final ObjectMapper mapper;
    private final McpJsonMapper json;
    private final Map<String, McpSyncTool> tools = new LinkedHashMap<>();
    private final Map<String, McpSyncResource> resources = new LinkedHashMap<>();
    private final Map<String, Resource> resourceDefs = new LinkedHashMap<>();
    private final Map<String, McpSyncResourceTemplate> templates = new LinkedHashMap<>();
    private final Map<String, McpSyncPrompt> prompts = new LinkedHashMap<>();
    private final HttpServletMcpTransport servlet;

    private record Resource(McpSchema.Resource def, McpSyncResource.Handler handler) {
    }

    private record Template(String uriTemplate, McpSyncResource.Handler handler) {
    }

    private final List<Template> templateHandlers = new ArrayList<>();

    public McpModernServer(List<Object> beans, ObjectMapper mapper, String name, String version, String endpoint) {
        this.mapper = mapper;
        this.json = new JacksonMcpJsonMapper(mapper);
        for (Object bean : beans) {
            scan(bean);
        }
        McpSyncToolRepository toolRepo = new McpSyncToolRepository() {
            @Override
            public ToolsPage list(McpRequestContext ctx, String cursor) {
                return ToolsPage.of(tools.values().stream().map(McpSyncTool::definition).toList());
            }

            @Override
            public McpSyncTool find(McpRequestContext ctx, String toolName) {
                return tools.get(toolName);
            }
        };
        McpSyncResourceRepository resourceRepo = new McpSyncResourceRepository() {
            @Override
            public ResourcesPage list(McpRequestContext ctx, String cursor) {
                return ResourcesPage.of(resourceDefs.values().stream().map(Resource::def).toList());
            }

            @Override
            public ResourceTemplatesPage listTemplates(McpRequestContext ctx, String cursor) {
                return ResourceTemplatesPage.of(templates.values().stream().map(McpSyncResourceTemplate::definition).toList());
            }

            @Override
            public McpSyncResource find(McpRequestContext ctx, String uri) {
                McpSyncResource exact = resources.get(uri);
                if (exact != null) {
                    return exact;
                }
                for (Template t : templateHandlers) {
                    if (!McpFeatureScanner.matchTemplate(t.uriTemplate(), uri).isEmpty()) {
                        return McpSyncResource.of(t.handler());
                    }
                }
                return null;
            }

            @Override
            public McpSyncResourceTemplate findTemplate(McpRequestContext ctx, String uriTemplate) {
                return templates.get(uriTemplate);
            }

            @Override
            public boolean supportsCompletion() {
                return true;
            }
        };
        McpSyncPromptRepository promptRepo = new McpSyncPromptRepository() {
            @Override
            public PromptsPage list(McpRequestContext ctx, String cursor) {
                return PromptsPage.of(prompts.values().stream().map(McpSyncPrompt::definition).toList());
            }

            @Override
            public McpSyncPrompt find(McpRequestContext ctx, String promptName) {
                return prompts.get(promptName);
            }

            @Override
            public boolean supportsCompletion() {
                return true;
            }
        };
        McpServer server = McpServer.builder()
                .serverInfo(new McpSchema.Implementation(name, null, version, null, null, null))
                .jsonMapper(json)
                .feature(ToolsFeature.ofSync(toolRepo))
                .feature(ResourcesFeature.ofSync(resourceRepo))
                .feature(PromptsFeature.ofSync(promptRepo))
                .build();
        this.servlet = HttpServletMcpTransport.builder(server).jsonMapper(json).endpoint(endpoint).build();
    }

    public jakarta.servlet.http.HttpServlet servlet() {
        return servlet;
    }

    // ---- scanning --------------------------------------------------------------------------------------------

    private void scan(Object bean) {
        List<Method> methods = McpServiceFactory.toolMethods(McpServiceFactory.userClass(bean));
        Map<String, List<Method>> promptCompletions = new HashMap<>();
        Map<String, List<Method>> uriCompletions = new HashMap<>();
        for (Method m : methods) {
            McpAnnotations.find(m, "McpComplete").ifPresent(a -> {
                String prompt = McpAnnotations.str(a, "prompt", "");
                (prompt.isEmpty() ? uriCompletions : promptCompletions)
                        .computeIfAbsent(prompt.isEmpty() ? McpAnnotations.str(a, "uri", "") : prompt, k -> new ArrayList<>())
                        .add(m);
            });
        }
        for (Method m : methods) {
            McpAnnotations.find(m, "McpTool").ifPresent(a -> addTool(bean, m, a));
            McpAnnotations.find(m, "McpResource").ifPresent(a -> addResource(bean, m, a, uriCompletions));
            McpAnnotations.find(m, "McpPrompt").ifPresent(a -> addPrompt(bean, m, a, promptCompletions));
        }
    }

    private static List<McpSchema.Icon> icons(Annotation a) {
        return McpAnnotations.annotations(a, "icons").stream().map(i -> new McpSchema.Icon(
                McpAnnotations.str(i, "src", null), McpAnnotations.str(i, "mimeType", null),
                List.of(McpAnnotations.strings(i, "sizes")), McpAnnotations.str(i, "theme", null))).toList();
    }

    private static McpSchema.Annotations resourceAnnotations(Annotation a) {
        McpAnnotations.ResourceAnnotations r = McpAnnotations.resourceAnnotations(a);
        return r == null ? null : new McpSchema.Annotations(r.audience().stream()
                .map(x -> McpSchema.Role.valueOf(x.toUpperCase())).toList(), r.priority(), r.lastModified());
    }

    private static void requireCapabilities(Annotation a, McpRequestContext ctx) {
        McpSchema.ClientCapabilities caps = ctx.clientCapabilities();
        for (String c : McpAnnotations.enumNames(a, "requiredCapabilities")) {
            boolean ok = caps != null && switch (c) {
                case "SAMPLING" -> caps.sampling() != null;
                case "ROOTS" -> caps.roots() != null;
                default -> caps.elicitation() != null;
            };
            if (!ok) {
                throw new McpMediatorException("Client capability required: " + c);
            }
        }
    }

    private void addTool(Object bean, Method m, Annotation a) {
        m.setAccessible(true);
        String name = McpAnnotations.str(a, "name", McpAnnotations.isSpringAi(a) ? m.getName()
                : McpUtils.convertCamelCaseToSnake(m.getName()));
        String description = McpAnnotations.str(a, "description", null);
        String schema;
        try {
            schema = McpMethodSchemaGenerator.of(mapper).generateSchemaForMethod(m);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new McpMediatorException("Failed to generate schema for " + m, e);
        }
        McpSchema.Tool.Builder tool = McpSchema.Tool.builder(name, json, schema)
                .description(description).title(McpAnnotations.str(a, "title", null)).icons(icons(a))
                .meta(McpAnnotations.meta(a));
        McpAnnotations.nested(a, "annotations").ifPresent(h -> tool.annotations(new McpSchema.ToolAnnotations(
                McpAnnotations.str(h, "title", null), McpAnnotations.bool(h, "readOnlyHint", false),
                McpAnnotations.bool(h, "destructiveHint", false), McpAnnotations.bool(h, "idempotentHint", false),
                McpAnnotations.bool(h, "openWorldHint", false))));
        tools.put(name, McpSyncTool.of(tool.build(), (ctx, req) -> {
            McpSchema.CallToolResult.Builder out = McpSchema.CallToolResult.builder();
            try {
                requireCapabilities(a, ctx);
                Object result = call(bean, m, req.arguments() == null ? Map.of() : req.arguments(),
                        McpCallContext.modern(ctx, req.inputResponses(), req.requestState(), json));
                if (result instanceof McpSchema.CallToolOutcome outcome) {
                    return McpSyncResponse.<McpSchema.CallToolOutcome>result(outcome); // e.g. InputRequiredResult
                }
                out.content(List.of(new McpSchema.TextContent(null, mapper.writeValueAsString(result), null)));
                if (result != null && !(result instanceof CharSequence) && !(result instanceof Number)
                        && !(result instanceof Boolean) && !(result instanceof Collection) && !result.getClass().isArray()
                        && !result.getClass().isEnum()) {
                    out.structuredContent(mapper.convertValue(result, Map.class));
                }
                out.isError(false);
            } catch (Exception e) {
                out.content(List.of(new McpSchema.TextContent(null,
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), null))).isError(true);
            }
            return McpSyncResponse.<McpSchema.CallToolOutcome>result(out.build());
        }));
    }

    private Object call(Object bean, Method m, Map<String, Object> args, McpCallContext context) throws Exception {
        McpCallContext.set(context);
        try {
            Object[] resolved = McpMethodArgumentResolver.resolveArguments(m, args, mapper);
            return McpUtils.awaitIfReactive(m.invoke(bean, resolved));
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof Exception ex ? ex : new McpMediatorException(String.valueOf(e.getCause()), e.getCause());
        } finally {
            McpCallContext.set(null);
        }
    }

    private void addResource(Object bean, Method m, Annotation a, Map<String, List<Method>> uriCompletions) {
        m.setAccessible(true);
        String uri = McpAnnotations.str(a, "uri", null);
        String name = McpAnnotations.str(a, "name", m.getName());
        String mime = McpAnnotations.str(a, "mimeType", "text/plain");
        McpSyncResource.Handler handler = (ctx, req) -> {
            requireCapabilities(a, ctx);
            Map<String, Object> vars = McpFeatureScanner.matchTemplate(uri, req.uri());
            Map<String, Object> args = new HashMap<>(vars);
            Parameter[] ps = m.getParameters();
            Object[] resolved = new Object[ps.length];
            try {
                McpCallContext.set(McpCallContext.modern(ctx, req.inputResponses(), req.requestState(), json));
                for (int i = 0; i < ps.length; i++) {
                    Class<?> t = ps[i].getType();
                    String n = McpUtils.getParameterName(ps[i]);
                    if (McpCallContext.isInjectable(ps[i])) resolved[i] = McpCallContext.injectedValue(ps[i]);
                    else if (t == McpSchema.ReadResourceRequest.class) resolved[i] = req;
                    else if (args.containsKey(n)) resolved[i] = mapper.convertValue(args.get(n), t);
                    else if (t == String.class && n.equals("uri")) resolved[i] = req.uri();
                }
                Object r = McpUtils.awaitIfReactive(m.invoke(bean, resolved));
                if (r instanceof McpSchema.ReadResourceOutcome outcome) {
                    return McpSyncResponse.<McpSchema.ReadResourceOutcome>result(outcome);
                }
                McpSchema.ReadResourceResult read = toRead(r, req.uri(), mime);
                long ttl = McpAnnotations.number(a, "ttlMs", 0);
                if (ttl > 0 && (read.ttlMs() == null || read.ttlMs() == 0)) {
                    read = McpSchema.ReadResourceResult.builder(read.contents()).ttlMs(ttl)
                            .cacheScope(McpSchema.CacheScope.valueOf(McpAnnotations.str(a, "cacheScope", "PRIVATE"))).build();
                }
                return McpSyncResponse.<McpSchema.ReadResourceOutcome>result(read);
            } catch (InvocationTargetException e) {
                throw new McpMediatorException("Failed to read " + req.uri() + ": " + e.getCause(), e.getCause());
            } catch (IllegalAccessException e) {
                throw new McpMediatorException("Failed to read " + req.uri(), e);
            } finally {
                McpCallContext.set(null);
            }
        };
        if (uri.contains("{")) {
            McpSchema.ResourceTemplate.Builder b = McpSchema.ResourceTemplate.builder(uri, name)
                    .title(McpAnnotations.str(a, "title", null)).description(McpAnnotations.str(a, "description", null))
                    .mimeType(mime).icons(icons(a)).meta(McpAnnotations.meta(a)).annotations(resourceAnnotations(a));
            McpSyncResourceTemplate tpl = McpSyncResourceTemplate.of(b.build());
            List<Method> completions = uriCompletions.get(uri);
            if (completions != null && !completions.isEmpty()) {
                tpl = tpl.withCompletionHandler(completionHandler(bean, completions.get(0)));
            }
            templates.put(uri, tpl);
            templateHandlers.add(new Template(uri, handler));
        } else {
            McpSchema.Resource.Builder b = McpSchema.Resource.builder(uri, name)
                    .title(McpAnnotations.str(a, "title", null)).description(McpAnnotations.str(a, "description", null))
                    .mimeType(mime).icons(icons(a)).meta(McpAnnotations.meta(a)).annotations(resourceAnnotations(a));
            long size = McpAnnotations.number(a, "size", -1);
            if (size >= 0) {
                b.size(size);
            }
            McpSchema.Resource def = b.build();
            resourceDefs.put(uri, new Resource(def, handler));
            resources.put(uri, McpSyncResource.of(handler));
        }
    }

    private static McpSchema.ReadResourceResult toRead(Object r, String uri, String mime) {
        if (r instanceof McpSchema.ReadResourceResult rr) return rr;
        List<McpSchema.ResourceContents> contents = new ArrayList<>();
        if (r instanceof Collection<?> c) {
            c.forEach(o -> contents.addAll(toRead(o, uri, mime).contents()));
        } else if (r instanceof byte[] bytes) {
            contents.add(new McpSchema.BlobResourceContents(uri, mime, Base64.getEncoder().encodeToString(bytes), null));
        } else if (r != null) {
            contents.add(new McpSchema.TextResourceContents(uri, mime, r.toString(), null));
        }
        return McpSchema.ReadResourceResult.builder(contents).build();
    }

    private void addPrompt(Object bean, Method m, Annotation a, Map<String, List<Method>> promptCompletions) {
        m.setAccessible(true);
        String name = McpAnnotations.str(a, "name", m.getName());
        List<McpSchema.PromptArgument> arguments = new ArrayList<>();
        for (Parameter p : m.getParameters()) {
            if (McpCallContext.isInjectable(p) || p.getType() == McpSchema.GetPromptRequest.class) continue;
            Optional<Annotation> arg = McpAnnotations.find(p, "McpArg");
            arguments.add(new McpSchema.PromptArgument(McpUtils.getParameterName(p),
                    arg.map(x -> McpAnnotations.str(x, "title", null)).orElse(null),
                    arg.map(x -> McpAnnotations.str(x, "description", null)).orElse(null),
                    arg.map(x -> McpAnnotations.bool(x, "required", true)).orElse(true)));
        }
        McpSchema.Prompt prompt = McpSchema.Prompt.builder(name).title(McpAnnotations.str(a, "title", null))
                .description(McpAnnotations.str(a, "description", null)).arguments(arguments).icons(icons(a)).meta(McpAnnotations.meta(a)).build();
        McpSyncPrompt p = McpSyncPrompt.of(prompt, (ctx, req) -> {
            requireCapabilities(a, ctx);
            Map<String, String> given = req.arguments() == null ? Map.of() : req.arguments();
            Parameter[] ps = m.getParameters();
            Object[] resolved = new Object[ps.length];
            try {
                McpCallContext.set(McpCallContext.modern(ctx, req.inputResponses(), req.requestState(), json));
                for (int i = 0; i < ps.length; i++) {
                    Class<?> t = ps[i].getType();
                    if (McpCallContext.isInjectable(ps[i])) resolved[i] = McpCallContext.injectedValue(ps[i]);
                    else if (t == McpSchema.GetPromptRequest.class) resolved[i] = req;
                    else resolved[i] = mapper.convertValue(given.get(McpUtils.getParameterName(ps[i])), t);
                }
                Object r = McpUtils.awaitIfReactive(m.invoke(bean, resolved));
                if (r instanceof McpSchema.GetPromptOutcome outcome) {
                    return McpSyncResponse.<McpSchema.GetPromptOutcome>result(outcome);
                }
                return McpSyncResponse.<McpSchema.GetPromptOutcome>result(toPrompt(r, prompt.description()));
            } catch (InvocationTargetException e) {
                throw new McpMediatorException("Failed to get prompt " + name + ": " + e.getCause(), e.getCause());
            } catch (IllegalAccessException e) {
                throw new McpMediatorException("Failed to get prompt " + name, e);
            } finally {
                McpCallContext.set(null);
            }
        });
        List<Method> completions = promptCompletions.get(name);
        if (completions != null && !completions.isEmpty()) {
            p = p.withCompletionHandler(completionHandler(bean, completions.get(0)));
        }
        prompts.put(name, p);
    }

    private static McpSchema.GetPromptResult toPrompt(Object r, String description) {
        if (r instanceof McpSchema.GetPromptResult g) return g;
        List<McpSchema.PromptMessage> messages = new ArrayList<>();
        if (r instanceof McpSchema.PromptMessage pm) messages.add(pm);
        else if (r instanceof Collection<?> c) c.forEach(o -> messages.addAll(toPrompt(o, description).messages()));
        else if (r != null) {
            messages.add(McpSchema.PromptMessage.builder(McpSchema.Role.ASSISTANT,
                    new McpSchema.TextContent(null, r.toString(), null)).build()); // Spring AI parity: String -> assistant
        }
        return McpSchema.GetPromptResult.builder(messages).description(description).build();
    }

    private McpSyncCompletionHandler completionHandler(Object bean, Method m) {
        m.setAccessible(true);
        return (ctx, req) -> {
            Parameter[] ps = m.getParameters();
            Object[] resolved = new Object[ps.length];
            for (int i = 0; i < ps.length; i++) {
                Class<?> t = ps[i].getType();
                if (t == McpSchema.CompleteRequest.class) resolved[i] = req;
                else if (t == McpSchema.CompleteRequest.CompleteArgument.class) resolved[i] = req.argument();
                else if (t == McpRequestContext.class) resolved[i] = ctx;
                else resolved[i] = t == String.class ? req.argument().value() : null;
            }
            try {
                Object r = m.invoke(bean, resolved);
                if (r instanceof McpSchema.CompleteResult cr) return cr;
                List<String> values = r instanceof Collection<?> c ? c.stream().map(String::valueOf).toList()
                        : r == null ? List.of() : List.of(r.toString());
                return new McpSchema.CompleteResult(new McpSchema.CompleteResult.Completion(values, values.size(), false), null, null);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new McpMediatorException("Completion failed: " + m.getName(), e);
            }
        };
    }
}
