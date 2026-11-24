package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.api.*;

import java.util.List;

/** Uses only the mediator's own annotations (no Spring). */
public class OwnAnnotatedFixture {
    public record Point(int x, int y) {}

    @McpTool(name = "add", description = "adds two numbers")
    public int add(@McpToolParam(description = "first") int a, @McpToolParam(description = "second") int b) {
        return a + b;
    }

    @McpTool(description = "moves a point")
    public Point movePoint(@McpToolParam Point point, @McpToolParam(required = false, description = "step") Integer step) {
        int s = step == null ? 1 : step;
        return new Point(point.x() + s, point.y() + s);
    }

    @McpResource(uri = "mem://greeting", name = "greeting", description = "static greeting")
    public String greeting() {
        return "hello";
    }

    @McpResource(uri = "mem://users/{id}/profile", name = "profile", mimeType = "application/json")
    public String profile(String id) {
        return "{\"id\":\"" + id + "\"}";
    }

    @McpPrompt(name = "review", description = "code review prompt")
    public String review(@McpArg(description = "language") String language,
                         @McpArg(required = false) String style) {
        return "Review this " + language + " code" + (style == null ? "" : " in " + style + " style");
    }

    @McpComplete(prompt = "review")
    public List<String> completeLanguage(String value) {
        return List.of("java", "javascript", "kotlin").stream().filter(s -> s.startsWith(value)).toList();
    }
}
