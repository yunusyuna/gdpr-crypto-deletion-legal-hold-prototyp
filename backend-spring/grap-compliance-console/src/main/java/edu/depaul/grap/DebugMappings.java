package edu.depaul.grap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class DebugMappings {

    @Bean
    CommandLineRunner showMappings(RequestMappingHandlerMapping mapping) {
        return args -> {
            System.out.println("\n=== REGISTERED REQUEST MAPPINGS ===");
            mapping.getHandlerMethods().forEach((k, v) -> System.out.println(k + " -> " + v));
            System.out.println("=== END MAPPINGS ===\n");
        };
    }
}
