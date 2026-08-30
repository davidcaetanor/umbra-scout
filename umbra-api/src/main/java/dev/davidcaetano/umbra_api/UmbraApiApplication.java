package dev.davidcaetano.umbra_api;

import dev.davidcaetano.umbra_api.config.UmbraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(UmbraProperties.class)
public class UmbraApiApplication {

    static void main(String[] args) {
        SpringApplication.run(UmbraApiApplication.class, args);
    }

}
