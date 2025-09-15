package com.example.hyu.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * Creates the application's OpenAPI configuration.
     *
     * Returns an OpenAPI instance with API metadata (title "HYU Core API", version "v1")
     * and a global JWT Bearer security requirement. A security scheme named
     * "bearer-jwt" is added to Components with HTTP type, "bearer" scheme and "JWT"
     * bearerFormat.
     *
     * @return configured OpenAPI instance with JWT Bearer security applied globally
     */
    @Bean
    public OpenAPI openAPI() {
        String scheme = "bearer-jwt";
        return new OpenAPI()
                .info(new Info().title("HYU Core API").version("v1"))
                .components(new Components().addSecuritySchemes(
                        scheme, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }


    /**
     * Creates a GroupedOpenApi bean that includes all API endpoints under the top-level
     * package "com.example".
     *
     * The resulting GroupedOpenApi is named "all", scans the "com.example" package (top-level)
     * and matches all request paths ("/**"), making every controller in that package available
     * to the generated OpenAPI documentation.
     *
     * @return a configured GroupedOpenApi that includes all endpoints in the "com.example" package
     */
    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("all")
                .packagesToScan("com.example") // ← 최상위로 올림
                .pathsToMatch("/**")           // ← 모든 경로
                .build();
    }
}