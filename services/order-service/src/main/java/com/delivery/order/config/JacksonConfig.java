package com.delivery.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for JSON serialization.
 * 
 * CONCEPT: @Configuration + @Bean
 * - @Configuration marks this class as a source of bean definitions
 * - @Bean methods create objects that Spring manages
 * - Other classes can then @Autowire/inject these beans
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure ObjectMapper with Java 8 time support.
     * 
     * Without this, Instant/LocalDateTime won't serialize properly.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Support for java.time.Instant, LocalDateTime, etc.
        mapper.registerModule(new JavaTimeModule());

        // Write dates as ISO strings, not timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
