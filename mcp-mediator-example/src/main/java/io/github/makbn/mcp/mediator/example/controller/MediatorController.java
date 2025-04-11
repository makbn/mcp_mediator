package io.github.makbn.mcp.mediator.example.controller;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpRequest;
import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import io.github.makbn.mcp.mediator.example.service.RequestHandlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MediatorController {

    private final McpMediator mediator;
    private final RequestHandlerService requestHandlerService;

    @Autowired
    public MediatorController(McpMediator mediator, RequestHandlerService requestHandlerService) {
        this.mediator = mediator;
        this.requestHandlerService = requestHandlerService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("handlersByImplementation", requestHandlerService.getHandlersByImplementation());
        return "index";
    }

    @PostMapping("/execute/{implementationName}/{requestType}")
    public String executeRequest(
            @PathVariable String implementationName,
            @PathVariable String requestType,
            @RequestParam Map<String, String> params,
            Model model) {
        try {
            McpRequestHandler handler = requestHandlerService.findHandler(implementationName, requestType);
            if (handler == null) {
                throw new IllegalArgumentException("No handler found for " + implementationName + ":" + requestType);
            }

            // Convert string parameters to appropriate types
            Map<String, Object> convertedParams = new HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                convertedParams.put(entry.getKey(), convertParameter(entry.getValue()));
            }

            // Create and execute request
            McpRequest request = handler.createRequest(convertedParams);
            Object result = mediator.execute(request);
            
            model.addAttribute("result", result);
            model.addAttribute("success", true);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("success", false);
        }
        return index(model);
    }

    private Object convertParameter(String value) {
        // Simple parameter type conversion
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Not an integer
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Not a double
        }
        return value;
    }
} 