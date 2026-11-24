package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.core.util.McpAnnotations;
import io.github.makbn.mcp.mediator.core.util.McpUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Collects client-side handler methods (sampling, elicitation, logging, progress, list-changed) from objects, using
 * the mediator's {@code @McpClientHandler} or Spring AI's {@code @McpSampling}/{@code @McpElicitation}/
 * {@code @McpLogging}/{@code @McpProgress}/{@code @McpToolListChanged}/{@code @McpResourceListChanged}/
 * {@code @McpPromptListChanged}, and applies them to an MCP SDK client.
 */
public final class McpClientHandlers {
    private static final Map<String, String> SPRING_AI_KINDS = Map.of(
            "McpSampling", "SAMPLING", "McpElicitation", "ELICITATION", "McpLogging", "LOGGING",
            "McpProgress", "PROGRESS", "McpToolListChanged", "TOOLS_CHANGED",
            "McpResourceListChanged", "RESOURCES_CHANGED", "McpPromptListChanged", "PROMPTS_CHANGED");

    private record Target(Object bean, Method method) {
        Object[] bind(Object arg) {
            return Binder.bind(method, arg);
        }

        Object call(Object arg) {
            try {
                method.setAccessible(true);
                return McpUtils.awaitIfReactive(method.invoke(bean, bind(arg)));
            } catch (InvocationTargetException e) {
                throw new McpMediatorException("Client handler failed: " + method, e.getCause());
            } catch (IllegalAccessException e) {
                throw new McpMediatorException("Client handler failed: " + method, e);
            }
        }
    }

    private static final class Binder {
        /** Binds a handler's parameters from the request/notification: whole object, or its fields by type. */
        static Object[] bind(Method m, Object arg) {
            java.lang.reflect.Parameter[] ps = m.getParameters();
            Object[] out = new Object[ps.length];
            int doubles = 0;
            int strings = 0;
            for (int i = 0; i < ps.length; i++) {
                Class<?> t = ps[i].getType();
                if (arg != null && t.isInstance(arg)) {
                    out[i] = arg;
                } else if (arg instanceof McpSchema.LoggingMessageNotification n) {
                    out[i] = t == McpSchema.LoggingLevel.class ? n.level() : t == String.class
                            ? (strings++ == 0 ? n.logger() : n.data()) : null;
                } else if (arg instanceof McpSchema.ProgressNotification n) {
                    if (McpAnnotations.find(ps[i], "McpProgressToken").isPresent()) {
                        out[i] = String.valueOf(n.progressToken());
                    } else if (t == Double.class || t == double.class) {
                        out[i] = doubles++ == 0 ? n.progress() : n.total();
                    } else if (t == String.class) {
                        out[i] = n.message();
                    } else if (t == Object.class) {
                        out[i] = n.progressToken();
                    }
                }
            }
            return out;
        }
    }

    private final Map<String, List<Target>> byKind = new HashMap<>();

    public static McpClientHandlers scan(Collection<?> beans) {
        return scan(beans, null);
    }

    /**
     * @param connection name of the connection the handlers are for; handlers declaring {@code clients} apply only
     *                   when it is listed. {@code null} applies every handler.
     */
    public static McpClientHandlers scan(Collection<?> beans, String connection) {
        McpClientHandlers handlers = new McpClientHandlers();
        for (Object bean : beans) {
            for (Method m : McpServiceFactory.toolMethods(McpServiceFactory.userClass(bean))) {
                for (Annotation a : m.getAnnotations()) {
                    String kind = kindOf(a);
                    if (kind != null && appliesTo(a, connection)) {
                        handlers.byKind.computeIfAbsent(kind, k -> new ArrayList<>()).add(new Target(bean, m));
                    }
                }
            }
        }
        return handlers;
    }

    private static boolean appliesTo(Annotation a, String connection) {
        String[] clients = McpAnnotations.strings(a, "clients");
        return connection == null || clients.length == 0 || Arrays.asList(clients).contains(connection);
    }

    private static String kindOf(Annotation a) {
        String name = a.annotationType().getName();
        if (name.equals("io.github.makbn.mcp.mediator.api.McpClientHandler")) {
            return String.valueOf(McpAnnotations.str(a, "value", null)).toUpperCase();
        }
        if (McpAnnotations.isSpringAiName(name)) {
            return SPRING_AI_KINDS.get(a.annotationType().getSimpleName());
        }
        return null;
    }

    public boolean isEmpty() {
        return byKind.isEmpty();
    }

    public boolean has(String kind) {
        return byKind.containsKey(kind);
    }

    public Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> sampling() {
        Target t = first("SAMPLING");
        return t == null ? null : r -> (McpSchema.CreateMessageResult) t.call(r);
    }

    public Function<McpSchema.ElicitFormRequest, McpSchema.ElicitResult> elicitation() {
        Target t = first("ELICITATION");
        return t == null ? null : r -> (McpSchema.ElicitResult) t.call(r);
    }

    public List<Consumer<McpSchema.LoggingMessageNotification>> logging() {
        return consumers("LOGGING");
    }

    public List<Consumer<McpSchema.ProgressNotification>> progress() {
        return consumers("PROGRESS");
    }

    public List<Consumer<List<McpSchema.Tool>>> toolsChanged() {
        return consumers("TOOLS_CHANGED");
    }

    public List<Consumer<List<McpSchema.Resource>>> resourcesChanged() {
        return consumers("RESOURCES_CHANGED");
    }

    public List<Consumer<List<McpSchema.Prompt>>> promptsChanged() {
        return consumers("PROMPTS_CHANGED");
    }

    /** Registers every discovered handler on the client builder. */
    public McpClient.SyncSpec applyTo(McpClient.SyncSpec spec) {
        if (has("SAMPLING")) spec.sampling(sampling());
        if (has("ELICITATION")) spec.elicitation(elicitation());
        if (has("LOGGING")) spec.loggingConsumers(logging());
        if (has("PROGRESS")) spec.progressConsumers(progress());
        toolsChanged().forEach(spec::toolsChangeConsumer);
        resourcesChanged().forEach(spec::resourcesChangeConsumer);
        promptsChanged().forEach(spec::promptsChangeConsumer);
        return spec;
    }

    private Target first(String kind) {
        List<Target> l = byKind.get(kind);
        return l == null ? null : l.get(0);
    }

    private <T> List<Consumer<T>> consumers(String kind) {
        List<Consumer<T>> out = new ArrayList<>();
        for (Target t : byKind.getOrDefault(kind, List.of())) {
            out.add(t::call);
        }
        return out;
    }
}
