package io.github.makbn.mcp.mediator.docker.server;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.McpServiceFactory;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.docker.internal.DockerClientService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

@Slf4j
public class DockerMcpServer {
    private static final String DOCKER_MCP_SERVER = "docker_mcp_server";
    private static final String DOCKER_MCP_SERVER_VER = "1.0.0.0";
    private static final String DEFAULT_DOCKER_HOST = "unix:///var/run/docker.sock";
    private static final int DEFAULT_MAX_CONNECTIONS = 100;

    public static void main(String[] args) {
        Options options = buildOptions();
        CommandLine cmd = parseOptions(args, options);
        try {
            configureLogging(cmd);
            String dockerHost = cmd.getOptionValue("docker-host", DEFAULT_DOCKER_HOST);
            String serverName = cmd.getOptionValue("server-name", DOCKER_MCP_SERVER);
            String serverVersion = cmd.getOptionValue("server-version", DOCKER_MCP_SERVER_VER);
            int maxConnections = Integer.parseInt(cmd.getOptionValue("max-connections", String.valueOf(DEFAULT_MAX_CONNECTIONS)));

            boolean tlsVerify = cmd.hasOption("tls-verify");
            String certPath = cmd.getOptionValue("cert-path");
            String dockerConfig = cmd.getOptionValue("docker-config");

            DockerClient dockerClient = initializeDockerClient(dockerHost, maxConnections, tlsVerify, certPath, dockerConfig);
            DockerClientService dockerClientService = new DockerClientService(dockerClient);

            DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
                    .createDefault()
                    .serverName(serverName)
                    .serverVersion(serverVersion)
                    .build());
            mediator.registerHandler(McpServiceFactory.create(dockerClientService)
                    .createForNonAnnotatedMethods(false)

                    .build());
            mediator.initialize();
        } catch (Exception e) {
            log.error("Failed to start Docker MCP Server: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void configureLogging(CommandLine cmd) {
        System.setProperty("DOCKER_MCP_LOG_FILE", cmd.getOptionValue("log-file", "logs/example.log"));
        System.setProperty("DOCKER_MCP_LOG_LEVEL", cmd.getOptionValue("log-level", "DEBUG"));
    }

    private static DockerClient intitializeDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();

        DockerHttpClient client = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .build();

        return DockerClientImpl.getInstance(config, client);
    }

    private static DockerClient initializeDockerClient(String dockerHost, int maxConnections, boolean tlsVerify, String certPath, String dockerConfig) {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost);

        if (tlsVerify) {
            if (certPath == null || certPath.isBlank()) {
                throw new IllegalArgumentException("TLS is enabled but cert path is not provided.");
            }
            builder.withDockerTlsVerify(true).withDockerCertPath(certPath);
        } else {
            builder.withDockerTlsVerify(false);
        }

        if (dockerConfig != null && !dockerConfig.isBlank()) {
            builder.withDockerConfig(dockerConfig);
        }

        DockerClientConfig config = builder.build();

        DockerHttpClient client = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(maxConnections)
                .build();

        return DockerClientImpl.getInstance(config, client);
    }


    private static Options buildOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "Print this help message");
        options.addOption("H", "docker-host", true, "Docker daemon host (default: unix:///var/run/docker.sock)");
        options.addOption("s", "server-name", true, "Server name (default: docker_mcp_server)");
        options.addOption("v", "server-version", true, "Server version (default: 1.0.0.0)");
        options.addOption("m", "max-connections", true, "Max Docker client connections (default: 100)");
        options.addOption(null, "tls-verify", false, "Enable TLS verification");
        options.addOption("c", "cert-path", true, "Path to Docker TLS certificates");
        options.addOption(null, "docker-config", true, "Path to override Docker config directory (~/.docker)");
        options.addOption(null, "log-file", true, "Path to log file (default: logs/docker_mcp_server.log)");
        options.addOption(null, "log-level", true, "Log level (TRACE, DEBUG, INFO, WARN, ERROR; default: DEBUG)");

        return options;
    }

    private static CommandLine parseOptions(String[] args, Options options) {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            log.error("Failed to parse command line options: {}", e.getMessage());
            printHelp(options);
            System.exit(1);
            return null; // Unreachable
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DockerMcpServer", options);
    }

}
