package com.freightfox.transporter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transporterOptimizerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FreightFox Transport Optimizer API")
                        .version("1.0.0")
                        .description("""
                                REST API for optimizing transporter assignments
                                across trade lanes.

                                The application accepts lane and transporter
                                quotes, then minimizes assignment cost while
                                maximizing the number of transporters used
                                within the configured limit.
                                """));
    }
}