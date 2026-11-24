package io.github.makbn.mcp.mediator.example;

import io.github.makbn.mcp.mediator.api.*;
import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;

import java.util.List;

/**
 * Plain Java (no Spring): exposes an ordinary object as tools, resources, prompts and completions using the
 * mediator's own annotations. The same class compiles unchanged with Spring AI's
 * {@code org.springaicommunity.mcp.annotation.*} annotations instead - both families are understood.
 */
public class AnnotatedBeanStdioExample {

    public static class Library {
        @McpTool(name = "add", description = "adds two numbers")
        public int add(@McpToolParam(description = "first") int a, @McpToolParam(description = "second") int b) {
            return a + b;
        }

        @McpResource(uri = "library://books/{isbn}", name = "book", mimeType = "application/json")
        public String book(String isbn) {
            return "{\"isbn\":\"" + isbn + "\"}";
        }

        @McpPrompt(name = "summarize", description = "summarize a book")
        public String summarize(@McpArg(description = "book title") String title) {
            return "Summarize the book " + title;
        }

        @McpComplete(prompt = "summarize")
        public List<String> completeTitle(String prefix) {
            return List.of("Dune", "Dracula", "Emma").stream().filter(t -> t.startsWith(prefix)).toList();
        }
    }

    public static void main(String[] args) {
        DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName("annotated_bean_example")
                .serverVersion("1.0.0")
                .build());
        mediator.registerBean(new Library()); // before initialize(): completions are fixed at startup
        mediator.initialize();
    }
}
