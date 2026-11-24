package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.core.util.McpAnnotations;
import io.github.makbn.mcp.mediator.core.util.McpUtils;
import io.modelcontextprotocol.server.McpServerFeatures.*;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans an object for {@code @McpResource}, {@code @McpPrompt} and {@code @McpComplete} methods (own annotations or
 * Spring AI's) and turns them into MCP SDK specifications. Tools go through {@link McpServiceFactory}.
 */
public final class McpFeatureScanner {

    public record Features(List<SyncResourceSpecification> resources,
                           List<SyncResourceTemplateSpecification> resourceTemplates,
                           List<SyncPromptSpecification> prompts,
                           List<SyncCompletionSpecification> completions) {
        public boolean isEmpty() {
            return resources.isEmpty() && resourceTemplates.isEmpty() && prompts.isEmpty() && completions.isEmpty();
        }
    }

    private McpFeatureScanner() {
    }

    static List<McpSchema.Icon> icons(Annotation a) {
        return McpAnnotations.annotations(a, "icons").stream().map(i -> new McpSchema.Icon(
                McpAnnotations.str(i, "src", null), McpAnnotations.str(i, "mimeType", null),
                List.of(McpAnnotations.strings(i, "sizes")), McpAnnotations.str(i, "theme", null))).toList();
    }

    static McpSchema.Annotations resourceAnnotations(Annotation a) {
        McpAnnotations.ResourceAnnotations r = McpAnnotations.resourceAnnotations(a);
        if (r == null) {
            return null;
        }
        return new McpSchema.Annotations(r.audience().stream()
                .map(x -> McpSchema.Role.valueOf(x.toUpperCase())).toList(), r.priority(), r.lastModified());
    }

    /** Fails the call when the connected client lacks a capability the feature declares as required. */
    static void requireCapabilities(Annotation a, McpSyncServerExchange ex) {
        if (ex == null) {
            return; // stateless: capabilities of the client are unknown
        }
        McpSchema.ClientCapabilities caps = ex.getClientCapabilities();
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

    public static Features scan(Object bean, ObjectMapper mapper) {
        List<SyncResourceSpecification> resources = new ArrayList<>();
        List<SyncResourceTemplateSpecification> templates = new ArrayList<>();
        List<SyncPromptSpecification> prompts = new ArrayList<>();
        List<SyncCompletionSpecification> completions = new ArrayList<>();

        for (Method m : McpServiceFactory.toolMethods(McpServiceFactory.userClass(bean))) {
            McpAnnotations.find(m, "McpResource").ifPresent(a -> {
                String uri = McpAnnotations.str(a, "uri", null);
                if (uri == null) {
                    throw new McpMediatorException("@McpResource requires a uri on " + m);
                }
                String name = McpAnnotations.str(a, "name", m.getName());
                String title = McpAnnotations.str(a, "title", null);
                String desc = McpAnnotations.str(a, "description", null);
                String mime = McpAnnotations.str(a, "mimeType", "text/plain");
                if (uri.contains("{")) {
                    templates.add(new SyncResourceTemplateSpecification(
                            McpSchema.ResourceTemplate.builder(uri, name).title(title).description(desc)
                                    .mimeType(mime).icons(icons(a)).meta(McpAnnotations.meta(a))
                                    .annotations(resourceAnnotations(a)).build(),
                            (ex, req) -> { requireCapabilities(a, ex); return readResource(bean, m, uri, mime, ex, req, mapper); }));
                } else {
                    resources.add(new SyncResourceSpecification(
                            McpSchema.Resource.builder(uri, name).title(title).description(desc)
                                    .mimeType(mime).icons(icons(a)).meta(McpAnnotations.meta(a)).annotations(resourceAnnotations(a))
                                    .size(McpAnnotations.number(a, "size", -1) >= 0 ? McpAnnotations.number(a, "size", -1) : null)
                                    .build(),
                            (ex, req) -> { requireCapabilities(a, ex); return readResource(bean, m, uri, mime, ex, req, mapper); }));
                }
            });
            McpAnnotations.find(m, "McpPrompt").ifPresent(a -> prompts.add(prompt(bean, m, a, mapper)));
            McpAnnotations.find(m, "McpComplete").ifPresent(a -> completions.add(completion(bean, m, a, mapper)));
        }
        return new Features(resources, templates, prompts, completions);
    }

    // ---- resources -------------------------------------------------------------------------------------------

    private static McpSchema.ReadResourceResult readResource(Object bean, Method m, String template, String mime,
                                                             McpSyncServerExchange ex,
                                                             McpSchema.ReadResourceRequest req, ObjectMapper mapper) {
        Map<String, Object> vars = matchTemplate(template, req.uri());
        Object[] args = new Object[m.getParameterCount()];
        Parameter[] ps = m.getParameters();
        for (int i = 0; i < ps.length; i++) {
            Class<?> t = ps[i].getType();
            String n = McpUtils.getParameterName(ps[i]);
            if (t == McpSchema.ReadResourceRequest.class) args[i] = req;
            else if (t == McpSyncServerExchange.class) args[i] = ex;
            else if (vars.containsKey(n)) args[i] = mapper.convertValue(vars.get(n), t);
            else if (t == String.class && n.equals("uri")) args[i] = req.uri();
            else args[i] = null;
        }
        Object result = invoke(bean, m, args);
        return toReadResult(result, req.uri(), mime);
    }

    /** Matches {@code file://{dir}/{name}} style templates; returns variable bindings (empty if no match). */
    static Map<String, Object> matchTemplate(String template, String uri) {
        List<String> names = new ArrayList<>();
        StringBuilder regex = new StringBuilder();
        Matcher vm = Pattern.compile("\\{([^}/]+)}").matcher(template);
        int last = 0;
        while (vm.find()) {
            regex.append(Pattern.quote(template.substring(last, vm.start()))).append("([^/]+)");
            names.add(vm.group(1));
            last = vm.end();
        }
        regex.append(Pattern.quote(template.substring(last)));
        Matcher m = Pattern.compile(regex.toString()).matcher(uri);
        Map<String, Object> vars = new LinkedHashMap<>();
        if (m.matches()) {
            for (int i = 0; i < names.size(); i++) vars.put(names.get(i), m.group(i + 1));
        }
        return vars;
    }

    @SuppressWarnings("unchecked")
    private static McpSchema.ReadResourceResult toReadResult(Object r, String uri, String mime) {
        if (r instanceof McpSchema.ReadResourceResult rr) return rr;
        List<McpSchema.ResourceContents> contents = new ArrayList<>();
        if (r instanceof McpSchema.ResourceContents rc) contents.add(rc);
        else if (r instanceof Collection<?> c) {
            for (Object o : c) {
                contents.addAll(toReadResult(o, uri, mime).contents());
            }
        } else if (r instanceof byte[] b) {
            contents.add(new McpSchema.BlobResourceContents(uri, mime, Base64.getEncoder().encodeToString(b)));
        } else if (r != null) {
            contents.add(new McpSchema.TextResourceContents(uri, mime, r.toString()));
        }
        return new McpSchema.ReadResourceResult(contents);
    }

    // ---- prompts ---------------------------------------------------------------------------------------------

    private static SyncPromptSpecification prompt(Object bean, Method m, Annotation a, ObjectMapper mapper) {
        List<McpSchema.PromptArgument> arguments = new ArrayList<>();
        for (Parameter p : m.getParameters()) {
            if (isInjected(p.getType())) continue;
            Optional<Annotation> arg = McpAnnotations.find(p, "McpArg");
            arguments.add(new McpSchema.PromptArgument(McpUtils.getParameterName(p),
                    arg.map(x -> McpAnnotations.str(x, "title", null)).orElse(null),
                    arg.map(x -> McpAnnotations.str(x, "description", null)).orElse(null),
                    arg.map(x -> McpAnnotations.bool(x, "required", true)).orElse(true)));
        }
        String name = McpAnnotations.str(a, "name", m.getName());
        McpSchema.Prompt prompt = McpSchema.Prompt.builder(name).title(McpAnnotations.str(a, "title", null))
                .description(McpAnnotations.str(a, "description", null)).arguments(arguments).icons(icons(a)).meta(McpAnnotations.meta(a)).build();
        return new SyncPromptSpecification(prompt, (ex, req) -> {
            requireCapabilities(a, ex);
            Map<String, Object> given = req.arguments() == null ? Map.of() : req.arguments();
            Parameter[] ps = m.getParameters();
            Object[] args = new Object[ps.length];
            for (int i = 0; i < ps.length; i++) {
                Class<?> t = ps[i].getType();
                if (t == McpSchema.GetPromptRequest.class) args[i] = req;
                else if (t == McpSyncServerExchange.class) args[i] = ex;
                else if (t == Map.class && !given.containsKey(McpUtils.getParameterName(ps[i]))) args[i] = given;
                else args[i] = mapper.convertValue(given.get(McpUtils.getParameterName(ps[i])), t);
            }
            return toPromptResult(invoke(bean, m, args), prompt.description());
        });
    }

    private static boolean isInjected(Class<?> t) {
        return t == McpSyncServerExchange.class || t == McpSchema.GetPromptRequest.class
                || t == McpSchema.ReadResourceRequest.class || t == McpSchema.CompleteRequest.class;
    }

    private static McpSchema.GetPromptResult toPromptResult(Object r, String description) {
        if (r instanceof McpSchema.GetPromptResult g) return g;
        List<McpSchema.PromptMessage> messages = new ArrayList<>();
        if (r instanceof McpSchema.PromptMessage pm) messages.add(pm);
        else if (r instanceof Collection<?> c) {
            for (Object o : c) {
                messages.addAll(toPromptResult(o, description).messages());
            }
        } else if (r != null) {
            messages.add(new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, new McpSchema.TextContent(r.toString()))); // Spring AI parity: String -> assistant message
        }
        return new McpSchema.GetPromptResult(description, messages);
    }

    // ---- completions -----------------------------------------------------------------------------------------

    private static SyncCompletionSpecification completion(Object bean, Method m, Annotation a, ObjectMapper mapper) {
        String prompt = McpAnnotations.str(a, "prompt", "");
        String uri = McpAnnotations.str(a, "uri", "");
        if (prompt.isEmpty() == uri.isEmpty()) {
            throw new McpMediatorException("@McpComplete needs exactly one of prompt/uri on " + m);
        }
        McpSchema.CompleteReference ref = prompt.isEmpty()
                ? new McpSchema.ResourceReference(uri) : new McpSchema.PromptReference(prompt);
        return new SyncCompletionSpecification(ref, (ex, req) -> {
            Parameter[] ps = m.getParameters();
            Object[] args = new Object[ps.length];
            for (int i = 0; i < ps.length; i++) {
                Class<?> t = ps[i].getType();
                if (t == McpSchema.CompleteRequest.class) args[i] = req;
                else if (t == McpSchema.CompleteRequest.CompleteArgument.class) args[i] = req.argument();
                else if (t == McpSyncServerExchange.class) args[i] = ex;
                else args[i] = t == String.class ? req.argument().value() : null;
            }
            Object r = invoke(bean, m, args);
            if (r instanceof McpSchema.CompleteResult cr) return cr;
            List<String> values = r instanceof Collection<?> c ? c.stream().map(String::valueOf).toList()
                    : r == null ? List.of() : List.of(r.toString());
            return new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(values, values.size(), false));
        });
    }

    private static Object invoke(Object bean, Method m, Object[] args) {
        try {
            m.setAccessible(true);
            return McpUtils.awaitIfReactive(m.invoke(bean, args));
        } catch (InvocationTargetException e) {
            throw new McpMediatorException("Failed to invoke " + m.getName() + ": " + e.getCause(), e.getCause());
        } catch (IllegalAccessException e) {
            throw new McpMediatorException("Failed to invoke " + m.getName(), e);
        }
    }
}
