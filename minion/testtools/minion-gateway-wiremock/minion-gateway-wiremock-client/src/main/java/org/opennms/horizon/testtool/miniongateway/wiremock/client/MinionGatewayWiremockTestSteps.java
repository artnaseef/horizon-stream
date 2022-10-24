package org.opennms.horizon.testtool.miniongateway.wiremock.client;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.messages.internal.com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.messages.internal.com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.opennms.cloud.grpc.minion.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MinionGatewayWiremockTestSteps {

    public static final String BASE_PATH = "/api";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionGatewayWiremockTestSteps.class);

    private Logger log = DEFAULT_LOGGER;

    public static int DEFAULT_HTTP_SOCKET_TIMEOUT = 30_000;

    private RetryUtils retryUtils;

    private String baseUrl;
    private String twinUpdateContent;

    private Response restAssuredResponse;

//========================================
// Constructor
//----------------------------------------

    public MinionGatewayWiremockTestSteps(RetryUtils retryUtils) {
        this.retryUtils = retryUtils;
    }


//========================================
// Cucumber Step Definitions
//----------------------------------------

    @Given("MOCK Minion Gateway Base URL in system property {string}")
    public void minionGatewayBaseURLInSystemProperty(String systemProperty) {
        baseUrl = System.getProperty(systemProperty);

        log.info("Using BASE URL {}", baseUrl);
    }


    @Given("MOCK twin update in resource file {string}")
    public void mockTwinUpdateInResourceFile(String resourcePath) throws Exception {
        twinUpdateContent = readResourceFile(resourcePath);
    }

    @Then("MOCK send twin update for topic {string} at location {string}")
    public void sendTwinUpdateForTopic(String topicName, String location) throws Exception {

        URL requestUrl = formatTwinUpdateUrl(topicName, location);

        RestAssuredConfig restAssuredConfig = createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig)
            ;

        restAssuredResponse =
            requestSpecification
                .header(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .body(twinUpdateContent)
                .post(requestUrl)
                .thenReturn()
        ;

        log.info("MOCK twin-update status-code={}; body={}", restAssuredResponse.getStatusCode(), restAssuredResponse.getBody().asString());
    }

    @Then("MOCK verify minion is connected with id {string} and location {string}")
    public void verifyMinionIsConnected(String minionId, String location) throws Exception {
        isMinionConnected(minionId, location, true);
    }

    @Then("MOCK wait for minion connection with id {string} and location {string} timeout {int}ms")
    public void waitForMinionConnection(String minionId, String location, int timeout) throws Exception {

        boolean found =
            retryUtils.retry(
                () -> isMinionConnected(minionId, location, false),
                result -> result,
                500,
                timeout,
                false
                );

        assertTrue("Minion is connected: minion-id=" + minionId + "; location=" + location, found);
    }

//========================================
// Internals
//----------------------------------------

    private String readResourceFile(String path) throws Exception {
        String content = "";

        try (InputStream inputStream = this.getClass().getResourceAsStream(path)) {
            content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        return content;
    }

    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
            );
    }

    private URL formatTwinUpdateUrl(String topic, String location) throws MalformedURLException {
        return formatUrl(BASE_PATH + "/twin-publish/" + topic + "/" + location);
    }

    private URL formatUrl(String path) throws MalformedURLException {
        return new URL(new URL(baseUrl), path);
    }

    private boolean isMinionConnected(String minionId, String location, boolean useAssert) {
        URL requestUrl;
        try {
            requestUrl = formatUrl(BASE_PATH + "/minions");
        } catch (MalformedURLException muExc) {
            throw new RuntimeException("URL format error", muExc);
        }

        RestAssuredConfig restAssuredConfig = createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig)
            ;

        restAssuredResponse =
            requestSpecification
                .header(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType())
                .get(requestUrl)
                .thenReturn()
        ;

        if (useAssert) {
            assertEquals(200, restAssuredResponse.getStatusCode());
        } else {
            if (restAssuredResponse.getStatusCode() != 200) {
                return false;
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List minions;

        try {
            minions = objectMapper.readValue(restAssuredResponse.getBody().asString(), List.class);
        } catch (JsonProcessingException exc) {
            throw new RuntimeException("failed to parse response", exc);
        }

        boolean found =
            minions.stream()
                .anyMatch(
                    (identity) ->
                        (location.equals(((Map) identity).get("location")) &&
                        (minionId.equals(((Map) identity).get("systemId"))))
                );

        if (useAssert) {
            assertTrue("Minion is connected: minion-id=" + minionId + "; location=" + location, found);
        }

        return found;
    }
}
