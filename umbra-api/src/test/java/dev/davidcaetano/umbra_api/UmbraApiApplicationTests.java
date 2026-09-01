package dev.davidcaetano.umbra_api;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UmbraApiApplicationTests extends IntegrationTestBase {

	@Test
	void contextLoads() {
	}

}
