package dev.davidcaetano.umbra_api.comum;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class CircuitBreakerResetExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        SpringExtension.getApplicationContext(context)
                .getBean(CircuitBreakerRegistry.class)
                .getAllCircuitBreakers()
                .forEach(CircuitBreaker::reset);
    }
}
