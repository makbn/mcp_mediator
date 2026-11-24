package io.github.makbn.mcp.mediator.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * Reads MCP annotations from either family: the mediator's own ({@code io.github.makbn.mcp.mediator.api}) or Spring AI's
 * ({@code org.springaicommunity.mcp.annotation}). The Spring AI ones are matched by name and read reflectively, so
 * neither Spring nor Spring AI is needed on the classpath.
 */
public final class McpAnnotations {
    private static final String OWN = "io.github.makbn.mcp.mediator.api.";
    /** Spring AI 1.x ships the annotations in the community package, Spring AI 2.x in org.springframework.ai. */
    private static final java.util.List<String> SPRING_AI = java.util.List.of(
            "org.springaicommunity.mcp.annotation.", "org.springframework.ai.mcp.annotation.");

    private McpAnnotations() {
    }

    /** Finds {@code @McpTool}/{@code @McpResource}/... (by simple name) of either family on the element. */
    public static Optional<Annotation> find(AnnotatedElement element, String simpleName) {
        for (Annotation a : element.getAnnotations()) {
            String n = a.annotationType().getName();
            if (n.equals(OWN + simpleName) || SPRING_AI.stream().anyMatch(p -> n.equals(p + simpleName))) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    /** @return true if the fully qualified name belongs to a Spring AI MCP annotation package (either generation). */
    public static boolean isSpringAiName(String name) {
        return SPRING_AI.stream().anyMatch(name::startsWith);
    }

    private static final java.util.Set<String> FEATURES =
            java.util.Set.of("McpTool", "McpResource", "McpPrompt", "McpComplete");

    /** @return true if the class (or a superclass) declares a method annotated with any MCP feature annotation. */
    public static boolean hasMcpMethods(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                for (Annotation a : m.getAnnotations()) {
                    String n = a.annotationType().getName();
                    if ((n.startsWith(OWN) || isSpringAiName(n))
                            && FEATURES.contains(n.substring(n.lastIndexOf('.') + 1))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isSpringAi(Annotation a) {
        return isSpringAiName(a.annotationType().getName());
    }

    /** Reads a String attribute; blank or missing yields {@code def}. */
    public static String str(Annotation a, String attr, String def) {
        Object v = attr(a, attr);
        if (v instanceof Enum<?> e) {
            return e.name();
        }
        return v instanceof String s && !s.isBlank() ? s : def;
    }

    /** Reads a String[] attribute; missing yields an empty array. */
    public static String[] strings(Annotation a, String attr) {
        return attr(a, attr) instanceof String[] arr ? arr : new String[0];
    }

    /** Reads an array-of-annotations attribute (e.g. {@code icons}); missing yields an empty list. */
    public static java.util.List<Annotation> annotations(Annotation a, String attr) {
        return attr(a, attr) instanceof Annotation[] arr ? java.util.List.of(arr) : java.util.List.of();
    }

    /** Reads an enum-array attribute (e.g. {@code requiredCapabilities}) as names. */
    public static java.util.List<String> enumNames(Annotation a, String attr) {
        return attr(a, attr) instanceof Object[] arr
                ? java.util.Arrays.stream(arr).map(o -> ((Enum<?>) o).name()).toList() : java.util.List.of();
    }

    public static long number(Annotation a, String attr, long def) {
        return attr(a, attr) instanceof Number n ? n.longValue() : def;
    }

    /**
     * Instantiates the {@code metaProvider} class of the annotation (Spring AI's {@code MetaProvider}) and returns its
     * {@code getMeta()} map, or null when absent/empty.
     */
    public static java.util.Map<String, Object> meta(Annotation a) {
        if (!(attr(a, "metaProvider") instanceof Class<?> type) || type.isInterface()) {
            return null;
        }
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object provider = ctor.newInstance();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> meta = (java.util.Map<String, Object>) type.getMethod("getMeta").invoke(provider);
            return meta == null || meta.isEmpty() ? null : meta;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** Resource annotations (audience/priority/lastModified) from either family; null when none declared. */
    public record ResourceAnnotations(java.util.List<String> audience, Double priority, String lastModified) {
    }

    public static ResourceAnnotations resourceAnnotations(Annotation a) {
        var nested = nested(a, "annotations");
        if (nested.isPresent()) { // Spring AI: always present, defaults audience=USER, priority=0.5
            Annotation h = nested.get();
            java.util.List<String> audience = enumNames(h, "audience");
            Double priority = attr(h, "priority") instanceof Number n ? n.doubleValue() : null;
            String lastModified = str(h, "lastModified", null);
            // Spring AI only advertises annotations that differ from the annotation defaults
            if (audience.equals(java.util.List.of("USER")) && Double.valueOf(0.5).equals(priority) && lastModified == null) {
                return null;
            }
            return new ResourceAnnotations(audience, priority, lastModified);
        }
        java.util.List<String> audience = java.util.List.of(strings(a, "audience"));
        Double priority = attr(a, "priority") instanceof Number n && n.doubleValue() >= 0 ? n.doubleValue() : null;
        String lastModified = str(a, "lastModified", null);
        return audience.isEmpty() && priority == null && lastModified == null ? null
                : new ResourceAnnotations(audience, priority, lastModified);
    }

    public static boolean bool(Annotation a, String attr, boolean def) {
        return attr(a, attr) instanceof Boolean b ? b : def;
    }

    /** Reads a nested annotation attribute (single value or first element of an array), e.g. {@code annotations()}. */
    public static Optional<Annotation> nested(Annotation a, String attr) {
        Object v = attr(a, attr);
        if (v instanceof Annotation n) {
            return Optional.of(n);
        }
        if (v instanceof Annotation[] arr && arr.length > 0) {
            return Optional.of(arr[0]);
        }
        return Optional.empty();
    }

    private static Object attr(Annotation a, String attr) {
        try {
            return a.annotationType().getMethod(attr).invoke(a);
        } catch (ReflectiveOperationException e) {
            return null; // attribute not present on this family
        }
    }
}
