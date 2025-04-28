package io.github.makbn.mcp.mediator.core.internal;

import io.github.makbn.mcp.mediator.core.util.McpUtils;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class to generate a human-readable description of a method's arguments and behavior.
 * <p>
 * It analyzes method parameters and extracts validation constraints from common Jakarta Validation annotations
 * such as {@link NotNull}, {@link Size}, {@link Min}, {@link Max}, etc.
 * <p>
 * Additional annotation extractors can be registered at runtime using {@link #registerAnnotation(Class, Function)}
 * to customize or extend the description behavior.
 * <p>
 * Typical usage:
 * <pre>
 * String description = McpMethodArgumentDescriber.describeMethod(myMethod);
 * </pre>
 *
 * @author Matt Akbarion
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpMethodArgumentDescriber {
    /** Set of supported validation annotations by default. */
    private static final List<Class<? extends Annotation>> VALIDATION_ANNOTATIONS = List.of(
            NotNull.class, NonNull.class, Size.class, Min.class, Max.class, Pattern.class, Positive.class,
            Negative.class
    );

    private static final Map<Class<? extends Annotation>, Function<? extends Annotation, String>>
            ANNOTATION_EXTRACTOR = new LinkedHashMap<>();

    static {
        // Register default extractors
        registerAnnotation(NotNull.class, ann -> "required (not null)");
        registerAnnotation(NonNull.class, ann -> "required (not null)");
        registerAnnotation(Size.class, ann -> "size between " + ann.min() + " and " + ann.max());
        registerAnnotation(Min.class, ann -> "minimum value " + ann.value());
        registerAnnotation(Max.class, ann -> "maximum value " + ann.value());
        registerAnnotation(Pattern.class, ann -> "must match regex '" + ann.regexp() + "'");
        registerAnnotation(Positive.class, ann -> "must be positive");
        registerAnnotation(Negative.class, ann -> "must be negative");
    }

    /**
     * Registers a custom annotation extractor.
     * <p>
     * You can register additional annotations along with a function that defines how to describe them.
     *
     * @param annotationClass the annotation class
     * @param extractor       the function to generate a description from the annotation instance
     * @param <T>             the type of annotation
     */
    public static <T extends Annotation> void registerAnnotation(Class<T> annotationClass, Function<T, String> extractor) {
        ANNOTATION_EXTRACTOR.put(annotationClass, extractor);
    }

    /**
     * Generates a human-readable description for a given method.
     * <p>
     * The output includes the method name, declaring class and package, a description of all its parameters,
     * constraints on parameters (if any), return type, and possible exceptions it may throw.
     *
     * @param method the method to describe
     * @return a formatted description string
     */
    public static String describeMethod(Method method) {
        StringBuilder sb = new StringBuilder();

        // Basic method info
        sb.append("Method '").append(method.getName()).append("' is a method inside [")
                .append(method.getDeclaringClass().getSimpleName())
                .append("] as part of [")
                .append(method.getDeclaringClass().getPackageName())
                .append("] that accepts ")
                .append(method.getParameterCount())
                .append(method.getParameterCount() == 1 ? " argument" : " arguments")
                .append(" as below:\n");

        // Describe parameters
        for (Parameter parameter : method.getParameters()) {
            sb.append("- ").append(McpUtils.getParameterName(parameter))
                    .append(" is a ").append(parameter.getType().getSimpleName());

            List<String> constraints = extractValidationConstraints(parameter);

            if (!constraints.isEmpty()) {
                sb.append(" with constraints: ").append(String.join(", ", constraints));
            }
            sb.append(".\n");
        }

        sb.append("It returns a ").append(method.getReturnType().getSimpleName()).append(" value");

        Class<?>[] exceptions = method.getExceptionTypes();
        if (exceptions.length > 0) {
            sb.append(" and may throw [")
                    .append(Arrays.stream(exceptions).map(Class::getSimpleName).collect(Collectors.joining(", ")))
                    .append("] exceptions");
        }
        sb.append(".");

        return sb.toString();
    }

    /**
     * Extracts validation-related constraints from a method parameter.
     *
     * @param parameter the parameter to inspect
     * @return a list of extracted constraint descriptions
     */
    private static List<String> extractValidationConstraints(Parameter parameter) {
        return Arrays.stream(parameter.getAnnotations())
                .filter(annotation -> VALIDATION_ANNOTATIONS.contains(annotation.annotationType()))
                .map(McpMethodArgumentDescriber::annotationToText)
                .toList();
    }

    /**
     * Converts a validation annotation to its textual description.
     * <p>
     * If a custom extractor is registered for the annotation, it is used; otherwise, the annotation's simple name is used.
     *
     * @param annotation the annotation instance
     * @param <M>        the type of annotation
     * @return a textual description
     */
    @NonNull
    @SuppressWarnings("unchecked")
    private static <M extends Annotation> String annotationToText(M annotation) {
        Function<M, String> extractor = (Function<M, String>) ANNOTATION_EXTRACTOR.get(annotation.annotationType());
        if (extractor != null) {
            return extractor.apply(annotation);
        }
        return annotation.annotationType().getSimpleName();
    }
}
