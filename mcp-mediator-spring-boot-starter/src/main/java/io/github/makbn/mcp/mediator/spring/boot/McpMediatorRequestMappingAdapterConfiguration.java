package io.github.makbn.mcp.mediator.spring.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

@Configuration
public class McpMediatorRequestMappingAdapterConfiguration implements ApplicationListener<ContextRefreshedEvent> {

    private McpMediator mcpMediator;

    @Override
    public void onApplicationEvent(@Nonnull ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping
                .getHandlerMethods();

        if (map == null || map.isEmpty()) {
            return;
        }

        ObjectMapper objectMapper = applicationContext.getBeanProvider(ObjectMapper.class).getIfAvailable(ObjectMapper::new);

        this.mcpMediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serializer(objectMapper)
                .serverName("spring_controllers_mcp_server")
                .serverVersion("1.0.0")
                .build());

        this.mcpMediator.registerHandler(McpSpringControllerFactory.create(map).build());
        this.mcpMediator.initialize();
    }

    public McpMediator getMcpMediator() {
        return mcpMediator;
    }
}
