package com.example.uniride.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Without {@code UserDetailsService}: authentication is only by JWT, there is no default user. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class UniRideBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniRideBackendApplication.class, args);
    }
}
