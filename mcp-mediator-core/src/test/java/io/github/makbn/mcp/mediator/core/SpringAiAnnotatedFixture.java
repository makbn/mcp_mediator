package io.github.makbn.mcp.mediator.core;

import org.springaicommunity.mcp.annotation.*;

import java.util.List;

/** Same behaviour as {@link OwnAnnotatedFixture} but written with Spring AI's native annotations. */
public class SpringAiAnnotatedFixture {
    @McpTool(name = "add", description = "adds two numbers")
    public int add(@McpToolParam(description = "first") int a, @McpToolParam(description = "second") int b) {
        return a + b;
    }

    @McpTool(description = "moves a point")
    public OwnAnnotatedFixture.Point movePoint(@McpToolParam OwnAnnotatedFixture.Point point,
                                               @McpToolParam(required = false, description = "step") Integer step) {
        int s = step == null ? 1 : step;
        return new OwnAnnotatedFixture.Point(point.x() + s, point.y() + s);
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
    public String review(@McpArg(name = "language", description = "language", required = true) String language,
                         @McpArg(name = "style", required = false) String style) {
        return "Review this " + language + " code" + (style == null ? "" : " in " + style + " style");
    }

    @McpComplete(prompt = "review")
    public List<String> completeLanguage(String value) {
        return List.of("java", "javascript", "kotlin").stream().filter(s -> s.startsWith(value)).toList();
    }
}
