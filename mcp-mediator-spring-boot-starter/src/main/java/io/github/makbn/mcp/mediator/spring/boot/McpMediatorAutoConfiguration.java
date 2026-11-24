package io.github.makbn.mcp.mediator.spring.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.util.McpAnnotations;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Exposes Spring beans carrying MCP annotations as an MCP server. Both annotation families work on the same beans:
 * the mediator's own ({@code io.github.makbn.mcp.mediator.api}) and Spring AI's
 * ({@code org.springaicommunity.mcp.annotation}). Enable with {@code mcp.mediator.enabled=true}. Backs off when the
 * application defines its own {@link McpMediator}; annotated beans are then registered on that one.
 */
@AutoConfiguration
@EnableConfigurationProperties(McpMediatorProperties.class)
@ConditionalOnProperty(prefix = "mcp.mediator", name = "enabled", havingValue = "true")
public class McpMediatorAutoConfiguration {

    /** Marks the mediator we created, so only that one is initialized by the registrar. */
    static final class AutoConfiguredMcpMediator extends DefaultMcpMediator {
        AutoConfiguredMcpMediator(io.github.makbn.mcp.mediator.core.configuration.McpMediatorDefaultConfiguration c) {
            super(c);
        }
    }

    @Bean
    @ConditionalOnMissingBean(McpMediator.class)
    DefaultMcpMediator mcpMediator(McpMediatorProperties properties, ObjectProvider<ObjectMapper> objectMapper) {
        return new AutoConfiguredMcpMediator(McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serializer(objectMapper.getIfAvailable(ObjectMapper::new))
                .transportType(properties.getTransport())
                .serverName(properties.getServerName())
                .serverVersion(properties.getServerVersion())
                .build());
    }

    /** Mounts the HTTP transport servlet (streamable HTTP or SSE) of the auto-configured mediator at {@code /mcp}. */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(
            type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "mcp.mediator", name = "transport", havingValue = "MODERN_HTTP")
    org.springframework.boot.web.servlet.ServletRegistrationBean<jakarta.servlet.http.HttpServlet> mcpModernServlet(
            DefaultMcpMediator mediator) {
        var registration = new org.springframework.boot.web.servlet.ServletRegistrationBean<>(
                mediator.getHttpServlet(), "/mcp", "/mcp/*");
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(
            type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "mcp.mediator", name = "transport", havingValue = "STATELESS_HTTP")
    org.springframework.boot.web.servlet.ServletRegistrationBean<jakarta.servlet.http.HttpServlet> mcpStatelessServlet(
            DefaultMcpMediator mediator) {
        var registration = new org.springframework.boot.web.servlet.ServletRegistrationBean<>(
                mediator.getHttpServlet(), "/mcp", "/mcp/*");
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(
            type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "mcp.mediator", name = "transport", havingValue = "STREAMABLE_HTTP")
    org.springframework.boot.web.servlet.ServletRegistrationBean<jakarta.servlet.http.HttpServlet> mcpHttpServlet(
            DefaultMcpMediator mediator) {
        var registration = new org.springframework.boot.web.servlet.ServletRegistrationBean<>(
                mediator.getHttpServlet(), "/mcp", "/mcp/*");
        registration.setAsyncSupported(true);
        return registration;
    }

    /** Mounts the SSE transport servlet ({@code /sse} and {@code /mcp/message}). */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(
            type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "mcp.mediator", name = "transport", havingValue = "SSE")
    org.springframework.boot.web.servlet.ServletRegistrationBean<jakarta.servlet.http.HttpServlet> mcpSseServlet(
            DefaultMcpMediator mediator) {
        var registration = new org.springframework.boot.web.servlet.ServletRegistrationBean<>(
                mediator.getHttpServlet(), "/sse", "/mcp/message");
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    static McpAnnotatedBeanRegistrar mcpAnnotatedBeanRegistrar(ConfigurableListableBeanFactory beanFactory,
                                                               ObjectProvider<McpMediator> mediator) {
        return new McpAnnotatedBeanRegistrar(beanFactory, mediator);
    }

    /** Registers every non-lazy singleton bean that declares MCP-annotated methods. */
    static final class McpAnnotatedBeanRegistrar implements SmartInitializingSingleton {
        private final ConfigurableListableBeanFactory beanFactory;
        private final ObjectProvider<McpMediator> mediator;

        McpAnnotatedBeanRegistrar(ConfigurableListableBeanFactory beanFactory, ObjectProvider<McpMediator> mediator) {
            this.beanFactory = beanFactory;
            this.mediator = mediator;
        }

        @Override
        public void afterSingletonsInstantiated() {
            McpMediator target = mediator.getIfAvailable();
            if (target == null) {
                return;
            }
            for (String name : beanFactory.getBeanDefinitionNames()) {
                BeanDefinition definition = beanFactory.getBeanDefinition(name);
                if (definition.isAbstract() || !definition.isSingleton() || definition.isLazyInit()) {
                    continue;
                }
                Class<?> type = beanFactory.getType(name);
                if (type == null) {
                    continue;
                }
                if (McpAnnotations.hasMcpMethods(type)) {
                    Object bean = beanFactory.getBean(name);
                    if (bean != target && AopProxyUtils.ultimateTargetClass(bean) != null) {
                        target.registerBean(bean);
                    }
                }
            }
            if (target instanceof AutoConfiguredMcpMediator) {
                target.initialize(); // after registration so completions are part of the server capabilities
            }
        }
    }
}
