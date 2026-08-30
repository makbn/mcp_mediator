package io.github.makbn.mcp.mediator.example;

import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorDefaultConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservabilityExample {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityExample.class);

    public static void main(String[] args) {
        log.info("Starting Observability Example...");

        // 1. Configure OpenTelemetry for Logging (SLF4J)
        // This will print spans and traces directly to standard out/log files.
        LoggingSpanExporter loggingSpanExporter = LoggingSpanExporter.create();
        
        // 2. Configure OpenTelemetry for Datadog / OTLP (Optional)
        // You would typically use environment variables like DD_API_KEY, but here is a programmatic example.
        /*
        OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("https://trace.agent.datadoghq.com")
                .addHeader("DD-API-KEY", "your-api-key")
                .build();
        */

        // 3. Build Tracer Provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                // Use SimpleSpanProcessor for logging in this example
                .addSpanProcessor(SimpleSpanProcessor.create(loggingSpanExporter))
                // For Datadog or production, use BatchSpanProcessor:
                // .addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build())
                .build();

        // 4. Initialize OpenTelemetry SDK
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
        
        log.info("OpenTelemetry configured. Building MCP Mediator configuration...");

        // 5. Pass OpenTelemetry to MCP Mediator
        McpMediatorDefaultConfiguration config = McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName("observable-mcp-server")
                .serverVersion("1.0.0")
                .openTelemetry(openTelemetry)
                .build();

        DefaultMcpMediator mediator = new DefaultMcpMediator(config);
        
        log.info("Mediator initialized with advanced observability! Any incoming MCP tool call will generate distributed traces.");
    }
}
