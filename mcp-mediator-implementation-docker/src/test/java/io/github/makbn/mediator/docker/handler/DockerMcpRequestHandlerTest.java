package io.github.makbn.mediator.docker.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DockerMcpRequestHandlerTest {

    @Test
    void connect_docker_engine() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .withDockerTlsVerify(true)
                .build();

        DockerHttpClient client = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, client);

        CreateContainerResponse response = dockerClient.createContainerCmd("nginx:latest")
                .exec();

        Assertions.assertNotNull(response.getId());
    }
}
