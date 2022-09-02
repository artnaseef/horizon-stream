package org.opennms.horizon.notifications;

import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = NotificationsApplication.class)
@TestPropertySource(locations = "classpath:application-integration-test.yml")
class NotificationsApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @LocalServerPort
    private Integer port;

	@Test
	void contextLoads() {
        System.out.println(environment.getProperty("spring.application.name"));
	}

    @Disabled
    @Test
    void callRest() {
        // TODO: Add a H2 database for testing.
        // Connect to it.
        // Add extra tests with mocked pager duty API somehow.
        Object obj = this.testRestTemplate.getForEntity("http://localhost:" + port + "/notifications/pagerDutyKey", ResponseEntity.class);
    }
}
