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

    @PostMapping("/execute/{implementationName}")
    public String executeRequest(
            @PathVariable String implementationName,
            @RequestParam Map<String, String> params,
            Model model) {
        try {
            McpRequestHandler<?, ?> handler = requestHandlerService.findHandler(implementationName);
            if (handler == null) {
                throw new IllegalArgumentException("No handler found for " + implementationName);
            }

            // Convert string parameters to appropriate types
            Map<String, Object> convertedParams = new HashMap<>(params);

            Object result = null;
            
            model.addAttribute("result", result);
            model.addAttribute("success", true);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("success", false);
        }
        return index(model);
    }
} 