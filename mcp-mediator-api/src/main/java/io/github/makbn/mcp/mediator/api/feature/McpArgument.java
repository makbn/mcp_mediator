package io.github.makbn.mcp.mediator.api.feature;

import java.lang.annotation.*;

/**
 * Annotation used to explicitly define the name of a method argument or field
 * when generating metadata or resolving parameters via reflection.
 * <p>
 * By default, Java compilers erase method parameter names unless the {@code -parameters} flag
 * is passed during compilation. This project configures the compiler to retain parameter names.
 * However, if the compiler option is not applied, or if for any reason retaining names is not guaranteed,
 * applying this annotation ensures the name is explicitly available at runtime.
 * <p>
 * Usage of this annotation is recommended as a fallback mechanism to avoid
 * relying solely on compiler-specific behavior.
 *
 * <pre>
 * {@code
 * public void exampleMethod(@McpArgument(name = "userId") String userId) {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @see java.lang.reflect.Parameter#getName()
 *
 * @author Matt Akbarian
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpArgument {
    String name();
}
