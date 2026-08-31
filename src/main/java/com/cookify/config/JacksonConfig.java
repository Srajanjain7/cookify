package com.cookify.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

/**
 * A request DTO's optional boolean field (e.g. LoginRequest.rememberMe)
 * being omitted from the JSON body is normal client behavior, not
 * malformed input -- it should default to false, not 500.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer lenientPrimitiveDefaultsCustomizer() {
        return builder -> builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    }
}
