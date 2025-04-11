package io.github.makbn.mcp.mediator.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    protected static final GenericContainer<?> dockerContainer = new GenericContainer<>(
            DockerImageName.parse("docker:latest"))
            .withCommand("sleep", "infinity");

    @BeforeEach
    void setUp() {
        // Start containers if not already running
        if (!dockerContainer.isRunning()) {
            dockerContainer.start();
        }
    }
} 