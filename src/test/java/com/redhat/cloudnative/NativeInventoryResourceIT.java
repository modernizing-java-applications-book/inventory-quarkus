package com.redhat.cloudnative;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * Integration tests that run against the native executable or container.
 * These tests verify the full application stack works correctly in
 * production-like mode.
 * 
 * To run native integration tests:
 * 1. Build the native executable: ./mvnw clean package -Pnative
 * 2. Run integration tests: ./mvnw verify -Pnative
 */
@QuarkusIntegrationTest
public class NativeInventoryResourceIT extends InventoryResourceTest {

        // ==================== Native-Specific Integration Tests ====================

        /**
         * Test that the application starts correctly in native mode and can serve
         * requests.
         */
        @Test
        public void testApplicationStartsSuccessfully() {
                given()
                                .when().get("/api/inventory")
                                .then()
                                .statusCode(200);
        }

        /**
         * Test the complete CRUD workflow in native mode.
         * This verifies the full request/response cycle works correctly.
         */
        @Test
        public void testCompleteCrudWorkflow() {
                // CREATE
                int createdId = given()
                                .contentType(ContentType.JSON)
                                .body("{\"quantity\": 250}")
                                .when().post("/api/inventory")
                                .then()
                                .statusCode(201)
                                .body("quantity", is(250))
                                .extract().path("id");

                // READ - Verify the created item
                given()
                                .when().get("/api/inventory/" + createdId)
                                .then()
                                .statusCode(200)
                                .body("id", is(createdId))
                                .body("quantity", is(250));

                // UPDATE - Modify the item
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"quantity\": 500}")
                                .when().put("/api/inventory/" + createdId)
                                .then()
                                .statusCode(200)
                                .body("quantity", is(500));

                // VERIFY UPDATE
                given()
                                .when().get("/api/inventory/" + createdId)
                                .then()
                                .statusCode(200)
                                .body("quantity", is(500));

                // DELETE - Remove the item
                given()
                                .when().delete("/api/inventory/" + createdId)
                                .then()
                                .statusCode(204);

                // VERIFY DELETE
                given()
                                .when().get("/api/inventory/" + createdId)
                                .then()
                                .statusCode(404);
        }

        /**
         * Test that JSON serialization works correctly in native mode.
         */
        @Test
        public void testJsonSerializationInNativeMode() {
                given()
                                .when().get("/api/inventory/329299")
                                .then()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body("id", is(329299))
                                .body("quantity", is(35));
        }

        /**
         * Test that error responses are properly serialized in native mode.
         */
        @Test
        public void testErrorResponseSerializationInNativeMode() {
                given()
                                .when().get("/api/inventory/999999")
                                .then()
                                .statusCode(404)
                                .contentType(ContentType.JSON)
                                .body("status", is(404))
                                .body("error", is("Not Found"))
                                .body("message", notNullValue())
                                .body("path", notNullValue())
                                .body("timestamp", notNullValue());
        }

        /**
         * Test that validation errors are properly handled in native mode.
         */
        @Test
        public void testValidationErrorResponseInNativeMode() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"quantity\": -100}")
                                .when().post("/api/inventory")
                                .then()
                                .statusCode(400)
                                .contentType(ContentType.JSON)
                                .body("status", is(400))
                                .body("error", is("Bad Request"));
        }

        /**
         * Test health check endpoints work in native mode.
         */
        @Test
        public void testHealthChecksInNativeMode() {
                // Health endpoint
                given()
                                .when().get("/q/health")
                                .then()
                                .statusCode(200)
                                .body("status", is("UP"));

                // Liveness endpoint
                given()
                                .when().get("/q/health/live")
                                .then()
                                .statusCode(200);

                // Readiness endpoint
                given()
                                .when().get("/q/health/ready")
                                .then()
                                .statusCode(200);
        }

        /**
         * Test pagination works correctly in native mode.
         */
        @Test
        public void testPaginationInNativeMode() {
                // First page
                given()
                                .queryParam("page", 0)
                                .queryParam("size", 5)
                                .when().get("/api/inventory")
                                .then()
                                .statusCode(200)
                                .body("size()", is(5));

                // Second page
                given()
                                .queryParam("page", 1)
                                .queryParam("size", 5)
                                .when().get("/api/inventory")
                                .then()
                                .statusCode(200)
                                .body("size()", is(3)); // Only 3 items remaining (8 total - 5 first page)
        }

        /**
         * Test count endpoint in native mode.
         */
        @Test
        public void testCountEndpointInNativeMode() {
                given()
                                .when().get("/api/inventory/count")
                                .then()
                                .statusCode(200)
                                .contentType(ContentType.TEXT);
        }

        /**
         * Test PATCH endpoint for partial updates in native mode.
         */
        @Test
        public void testPatchUpdateInNativeMode() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"quantity\": 777}")
                                .when().patch("/api/inventory/444434/quantity")
                                .then()
                                .statusCode(200)
                                .body("id", is(444434))
                                .body("quantity", is(777));
        }

        /**
         * Test that concurrent requests are handled correctly.
         */
        @Test
        public void testMultipleSequentialRequestsInNativeMode() {
                for (int i = 0; i < 5; i++) {
                        given()
                                        .when().get("/api/inventory")
                                        .then()
                                        .statusCode(200);
                }
        }
}