package com.example.demomessage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demomessage.config.DemoProperties;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoController.class)
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoProperties demoProperties;

    @Test
    void healthShouldReturnStatusOkAndServiceName() throws Exception {
        when(demoProperties.getServiceName()).thenReturn("demo-message-service");

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("demo-message-service"));
    }

    @Test
    void messageShouldReturnCurrentDemoConfig() throws Exception {
        when(demoProperties.getServiceName()).thenReturn("demo-message-service");
        when(demoProperties.getVersion()).thenReturn("1.0.0");
        when(demoProperties.getEnvironment()).thenReturn("production");
        when(demoProperties.getMessage()).thenReturn("Hello from approved deployment");

        mockMvc.perform(get("/message"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("demo-message-service"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.environment").value("production"))
                .andExpect(jsonPath("$.message").value("Hello from approved deployment"));
    }
}
