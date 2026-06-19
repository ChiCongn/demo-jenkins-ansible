package com.example.demomessage;

import com.example.demomessage.config.DemoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DemoProperties.class)
public class DemoMessageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoMessageServiceApplication.class, args);
    }
}
