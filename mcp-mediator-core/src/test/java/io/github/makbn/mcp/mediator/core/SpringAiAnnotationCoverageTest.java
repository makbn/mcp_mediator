package io.github.makbn.mcp.mediator.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard against silent gaps: every attribute of every Spring AI 2.x MCP annotation must be listed here as handled by the
 * mediator (with the test that proves it) or explicitly as ignored. A new upstream attribute fails this test until a
 * decision is made.
 */
class SpringAiAnnotationCoverageTest {
    private static final String PKG = "org.springframework.ai.mcp.annotation.";

    private static final List<String> CLIENTS = List.of("clients");

    /** annotation -> attributes handled (see McpAnnotations/McpFeatureScanner/McpModernServer/McpClientHandlers). */
    private static final Map<String, List<String>> HANDLED = Map.ofEntries(
            Map.entry("McpArg", List.of("description", "name", "required", "title")),
            Map.entry("McpComplete", List.of("prompt", "uri")),
            Map.entry("McpElicitation", CLIENTS),
            Map.entry("McpLogging", CLIENTS),
            Map.entry("McpProgress", CLIENTS),
            Map.entry("McpSampling", CLIENTS),
            Map.entry("McpToolListChanged", CLIENTS),
            Map.entry("McpResourceListChanged", CLIENTS),
            Map.entry("McpPromptListChanged", CLIENTS),
            Map.entry("McpIcon", List.of("mimeType", "sizes", "src", "theme")),
            Map.entry("McpPrompt", List.of("description", "icons", "metaProvider", "name", "requiredCapabilities", "title")),
            Map.entry("McpResource", List.of("annotations", "cacheScope", "description", "icons", "metaProvider", "mimeType",
                    "name", "requiredCapabilities", "size", "title", "ttlMs", "uri")),
            Map.entry("McpTool", List.of("annotations", "description", "generateOutputSchema", "icons", "metaProvider", "name",
                    "requiredCapabilities", "title")),
            Map.entry("McpToolParam", List.of("description", "header", "required")),
            Map.entry("McpProgressToken", List.of()),
            Map.entry("McpResource$McpAnnotations", List.of("audience", "lastModified", "priority")),
            Map.entry("McpTool$McpAnnotations", List.of("destructiveHint", "idempotentHint", "openWorldHint", "readOnlyHint", "title")));

    /** Not annotations (types/enums) living in the same package. */
    private static final Set<String> NON_ANNOTATIONS = Set.of("McpClientCapability", "McpMeta");

    @Test
    void everySpringAiAnnotationAttributeIsAccountedFor() throws Exception {
        URL url = Class.forName(PKG + "McpTool").getProtectionDomain().getCodeSource().getLocation();
        Set<String> found = new TreeSet<>();
        try (JarFile jar = new JarFile(new File(url.toURI()))) {
            jar.stream().map(e -> e.getName()).filter(n -> n.matches("org/springframework/ai/mcp/annotation/Mcp[A-Za-z$]*\\.class"))
                    .forEach(n -> found.add(n.substring(n.lastIndexOf('/') + 1, n.length() - 6)));
        }
        List<String> problems = new ArrayList<>();
        for (String simple : found) {
            if (NON_ANNOTATIONS.contains(simple) || simple.matches(".*\\$[0-9]+")) continue;
            Class<?> c = Class.forName(PKG + simple);
            if (!c.isAnnotation()) continue;
            List<String> handled = HANDLED.get(simple);
            if (handled == null) {
                problems.add("new annotation not covered: " + simple);
                continue;
            }
            Set<String> attrs = new TreeSet<>();
            for (Method m : c.getDeclaredMethods()) attrs.add(m.getName());
            for (String a : attrs) if (!handled.contains(a)) problems.add(simple + "." + a + " is not handled");
            for (String h : handled) if (!attrs.contains(h)) problems.add(simple + "." + h + " no longer exists (stale)");
        }
        for (String expected : HANDLED.keySet()) if (!found.contains(expected)) problems.add("expected annotation missing: " + expected);
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
