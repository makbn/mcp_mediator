package io.github.makbn.mcp.mediator.spring.boot;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import jakarta.annotation.Nonnull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

@Configuration
//@ConditionalOnProperty(name = "mcp.mediator.endpoint.mapping.type", value = "request-mapping")
public class McpMediatorRequestMappingAdapterConfiguration implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(@Nonnull ContextRefreshedEvent event) {
        McpMediator mcpMediator = new DefaultMcpMediator();

        ApplicationContext applicationContext = event.getApplicationContext();
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping
                .getHandlerMethods();

        map.forEach((mappingInfo, method) -> {
            System.out.printf("mappingInfo: %s\n", mappingInfo);
            System.out.printf("method   : %s\n", method);
        });
    }
}
