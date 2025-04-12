package io.github.makbn.mcp.mediator.example.service;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RequestHandlerService {

    private final McpMediator mediator;

    @Autowired
    public RequestHandlerService(McpMediator mediator) {
        this.mediator = mediator;
    }

    public List<McpRequestHandler<?, ?>> getAllHandlers() {
        return mediator.getHandlers();
    }

    public Map<String, List<McpRequestHandler<?, ?>>> getHandlersByImplementation() {
        return mediator.getHandlers().stream()
                .collect(Collectors.groupingBy(McpRequestHandler::getName));
    }

    public McpRequestHandler<?, ?> findHandler(String implementationName) {
        return mediator.getHandlers().stream()
                .filter(handler -> handler.getName().equals(implementationName))
                .findFirst()
                .orElse(null);
    }
} 