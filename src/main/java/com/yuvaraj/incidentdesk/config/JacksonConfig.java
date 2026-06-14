package com.yuvaraj.incidentdesk.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /** Enables tri-state JSON (absent vs explicit null) used by PATCH request DTOs. */
    @Bean
    public JsonNullableModule jsonNullableModule() {
        return new JsonNullableModule();
    }
}
