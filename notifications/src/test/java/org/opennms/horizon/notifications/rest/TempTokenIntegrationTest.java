package org.opennms.horizon.notifications.rest;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.notifications.NotificationsApplication;
import org.opennms.horizon.notifications.api.PagerDutyAPI;
import org.opennms.horizon.shared.dto.notifications.PagerDutyConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.Assert.assertEquals;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = NotificationsApplication.class)
@TestPropertySource(locations = "classpath:application.yml")
class TempTokenIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @SpyBean
    private PagerDutyAPI pagerDutyAPI;

    @LocalServerPort
    private Integer port;

    @Test
    void callSaveConfig() throws Exception {
        String integrationKey = "not_verified";

        saveConfig(integrationKey);
    }

    private void saveConfig(String integrationKey) {
        PagerDutyConfigDTO config = new PagerDutyConfigDTO(integrationKey);
        HttpHeaders headers = new HttpHeaders();
        String accessToken = "";
        accessToken = "";
        accessToken = "";
        accessToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICItSENNdWswaXNtS1YxT09kZzhEbE9WQ0hUZW1nWUl0eVd3bjUyOFc3azlRIn0.eyJleHAiOjE2NjU1NzM1MzYsImlhdCI6MTY2NTU2NzUzNiwianRpIjoiZTk5NDU2ZDYtYWMyZS00ZWQyLWI2MjUtYmFmZTI3MzI2MGMwIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MTIzL2F1dGgvcmVhbG1zL29wZW5ubXMiLCJzdWIiOiJlOWRkM2IzZS1jMjNhLTQ0ZTQtYjYwMC1jYjI0YzdjNDlkMGUiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhZG1pbi1jbGkiLCJzZXNzaW9uX3N0YXRlIjoiOWVmNjE5YjMtY2I1OC00ZTk4LThhMGMtZDAyM2RhNTg2NGNiIiwiYWNyIjoiMSIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJzaWQiOiI5ZWY2MTliMy1jYjU4LTRlOTgtOGEwYy1kMDIzZGE1ODY0Y2IiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6ImFkbWluIiwiZW1haWwiOiJhZG1pbkB0ZXN0Lm9wZW5ubXMub3JnIn0.wyLDQDAvZiA1tZlgQuFAUM5kwfIJWEBa_5bma6k2y5Rll2fgmCaw1B4vRdyOxQzCdSoXKBnGm_tqvihJ_TJv_3eP2TgbaBqsnT4N6atYzQR0IdbHyi2a9R5uQZaZtiPcozVJJ5rmhSVxYoMHlQzz_RFRf43NhZk-ermyU-sp5OKaqA7eiQgU9s-MfTpqAG77ZIo0S7HoM82CCA9_2TilzyZ98h8_ze8_NeJYe74aqy0tJxnI2Zr6xEpSw-q9qZzCUqIc3h4ABlOhPS7_d2FPeuHPAeDMwVzyIpxi-sT4u1szZMnwH5-6amjO8cpDEsCiTpHUaEvtO5rkPHidRk8UBg";
        headers.set("Authorization", "Bearer "+accessToken);
        HttpEntity<PagerDutyConfigDTO> request = new HttpEntity<>(config, headers);

        ResponseEntity<String> response = this.testRestTemplate
            .postForEntity("http://localhost:" + port + "/notifications/config", request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        //assertEquals("OK", response.getBody());

        //verifyConfigTable(integrationKey);
    }

    private void verifyConfigTable(String integrationKey) {
        String sql = "SELECT integrationKey FROM pager_duty_config";
        List<PagerDutyConfigDTO> configList = null;
        configList = jdbcTemplate.query(
            sql,
            (rs, rowNum) ->
                new PagerDutyConfigDTO(
                    rs.getString("integrationKey")
                )
        );

        PagerDutyConfigDTO config = configList.get(0);

        assertEquals(integrationKey, config.getIntegrationkey());
    }
}
