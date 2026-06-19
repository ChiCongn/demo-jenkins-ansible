package com.example.demomessage.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class DemoPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "demo.service-name=demo-message-service",
                    "demo.version=1.0.0",
                    "demo.environment=production",
                    "demo.message=Hello from approved deployment"
            );

    @Test
    void shouldBindDemoProperties() {
        contextRunner.run(context -> {
            DemoProperties properties = context.getBean(DemoProperties.class);

            assertThat(properties.getServiceName()).isEqualTo("demo-message-service");
            assertThat(properties.getVersion()).isEqualTo("1.0.0");
            assertThat(properties.getEnvironment()).isEqualTo("production");
            assertThat(properties.getMessage()).isEqualTo("Hello from approved deployment");
        });
    }

    @Configuration
    @EnableConfigurationProperties(DemoProperties.class)
    static class TestConfiguration {
    }
}
