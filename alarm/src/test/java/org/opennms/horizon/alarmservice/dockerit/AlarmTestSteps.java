/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 ******************************************************************************/

package org.opennms.horizon.alarmservice.dockerit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@Slf4j
public class AlarmTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    //
    // Test Configuration
    //
    private String applicationBaseUrl;
    private String kafkaRestBaseUrl;

    private String kafaBootstrapServers;

    //
    // Test Runtime Data
    //
    private Response restAssuredResponse;
    private Response rememberedRestAssuredResponse;

//========================================
// Gherkin Rules
//========================================

    @Given("Application Base URL in system property {string}")
    public void applicationBaseURLInSystemProperty(String systemProperty) {
        this.applicationBaseUrl = System.getProperty(systemProperty);

        log.info("Using BASE URL {}", this.applicationBaseUrl);
    }

    @Given("Kafka Rest Server URL in system property {string}")
    public void kafkaRestServerURLInSystemProperty(String systemProperty) {
        this.kafkaRestBaseUrl = System.getProperty(systemProperty);

        log.info("Using KAFKA REST BASE URL {}", this.kafkaRestBaseUrl);
    }

    @Given("Kafka Boot Servers in system property {string}")
    public void kafkaBootstrapServersInSystemProperty(String systemProperty) {
        this.kafaBootstrapServers = System.getProperty(systemProperty);

        log.info("################### Kafka Boostrap-servers {}", this.kafaBootstrapServers);
    }
    
    @Then("Send POST request to application at path {string}")
    public void sendPOSTRequestToApplicationAtPath(String path) throws Exception {
        commonSendPOSTRequestToApplication(path);
    }

    @Then("Send message to Kafka at topic {string}")
    public void sendMessageToKafkaAtTopic(String topic) throws Exception {
        URL requestUrl = new URL(new URL(this.kafkaRestBaseUrl), "/topics/" + topic);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        String body = formatKafkaRestProducerMessageBody("blah");

        requestSpecification =
            requestSpecification
                .body(body)
                .header("Content-Type", "application/vnd.kafka.binary.v2+json")
                .header("Accept", "application/vnd.kafka.v2+json")
                ;

        restAssuredResponse =
            requestSpecification
                .post(requestUrl)
                .thenReturn()
        ;
    }

    @Then("delay")
    public void delay() throws InterruptedException{
        Thread.sleep(6000);
        // Thread.sleep(3600000);
    }

    @Then("Verify the HTTP response code is {int}")
    public void verifyTheHTTPResponseCodeIs(int expectedStatusCode) {
        assertEquals(expectedStatusCode, restAssuredResponse.getStatusCode());
    }

    @Then("Remember response body for later comparison")
    public void rememberResponseBodyForLaterComparison() {
        rememberedRestAssuredResponse = restAssuredResponse;
    }

//========================================
// Utility Rules
//----------------------------------------

    @Then("^DEBUG dump the response body$")
    public void debugDumpTheResponseBody() {
        this.log.info("RESPONSE BODY = {}", restAssuredResponse.getBody().asString());
    }

//========================================
// Internals
//----------------------------------------

    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
            );
    }

    private void commonSendPOSTRequestToApplication(String path) throws MalformedURLException {
        URL requestUrl = new URL(new URL(this.applicationBaseUrl), path);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        restAssuredResponse =
            requestSpecification
                .post(requestUrl)
                .thenReturn()
        ;
    }

    private String formatKafkaRestProducerMessageBody(String payload) throws JsonProcessingException {
        KafkaRestRecord record = new KafkaRestRecord();

        byte[] encoded = Base64.getEncoder().encode(payload.getBytes(StandardCharsets.UTF_8));
        record.setValue(new String(encoded, StandardCharsets.UTF_8));

        KafkaRestProducerRequest request = new KafkaRestProducerRequest();
        request.setRecords(new KafkaRestRecord[] { record });

        return new ObjectMapper().writeValueAsString(request);
    }

//========================================
//
//----------------------------------------

    private static class KafkaRestProducerRequest {
        private KafkaRestRecord[] records;

        public KafkaRestRecord[] getRecords() {
            return records;
        }

        public void setRecords(KafkaRestRecord[] records) {
            this.records = records;
        }
    }

    private static class KafkaRestRecord {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
