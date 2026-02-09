package com.redhat.cloudnative;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class InventoryResourceV1Test {

    // ==================== GET /api/v1/inventory Tests ====================

    @Test
    public void testV1ListAllInventoryDefaultPagination() {
        given()
                .when().get("/api/v1/inventory")
                .then()
                .statusCode(200)
                .body("page", is(0))
                .body("size", is(20));
    }

    @Test
    public void testV1ListInventoryWithPagination() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 3)
                .when().get("/api/v1/inventory")
                .then()
                .statusCode(200)
                .body("data.size()", is(3))
                .body("page", is(0));
    }

    @Test
    public void testV1ListAllWithoutPagination() {
        given()
                .when().get("/api/v1/inventory/all")
                .then()
                .statusCode(200);
    }

    @Test
    public void testV1CountInventory() {
        given()
                .when().get("/api/v1/inventory/count")
                .then()
                .statusCode(200);
    }

    // ==================== GET /api/v1/inventory/{id} Tests ====================

    @Test
    public void testV1GetInventoryById() {
        given()
                .when().get("/api/v1/inventory/329299")
                .then()
                .statusCode(200)
                .body("id", is(329299))
                .body("quantity", is(35));
    }

    @Test
    public void testV1GetInventoryByIdNotFound() {
        given()
                .when().get("/api/v1/inventory/999999")
                .then()
                .statusCode(404)
                .body("status", is(404))
                .body("error", is("Not Found"));
    }

    // ==================== GET /api/v1/inventory/product/{productId} Tests
    // ====================

    @Test
    public void testV1GetInventoryByProductId() {
        given()
                .when().get("/api/v1/inventory/product/1002")
                .then()
                .statusCode(200)
                .body("productId", is(1002))
                .body("quantity", is(35));
    }

    @Test
    public void testV1GetInventoryByProductIdNotFound() {
        given()
                .when().get("/api/v1/inventory/product/999999")
                .then()
                .statusCode(404);
    }

    // ==================== POST /api/v1/inventory Tests ====================

    @Test
    public void testV1CreateInventory() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"productId\": 5001, \"quantity\": 100}")
                .when().post("/api/v1/inventory")
                .then()
                .statusCode(201)
                .body("quantity", is(100))
                .body("productId", is(5001))
                .header("Location", containsString("/api/v1/inventory/"));
    }

    @Test
    public void testV1CreateInventoryWithNegativeQuantity() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"productId\": 5002, \"quantity\": -10}")
                .when().post("/api/v1/inventory")
                .then()
                .statusCode(400);
    }

    // ==================== PUT /api/v1/inventory/{id} Tests ====================

    @Test
    public void testV1UpdateInventory() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"productId\": 1004, \"quantity\": 999}")
                .when().put("/api/v1/inventory/165613")
                .then()
                .statusCode(200)
                .body("id", is(165613))
                .body("quantity", is(999));
    }

    @Test
    public void testV1UpdateInventoryNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"productId\": 9999, \"quantity\": 100}")
                .when().put("/api/v1/inventory/999999")
                .then()
                .statusCode(404);
    }

    // ==================== PATCH /api/v1/inventory/{id}/quantity Tests
    // ====================

    @Test
    public void testV1UpdateQuantity() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"quantity\": 600}")
                .when().patch("/api/v1/inventory/329199/quantity")
                .then()
                .statusCode(200)
                .body("id", is(329199))
                .body("quantity", is(600));
    }

    @Test
    public void testV1UpdateQuantityNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"quantity\": 100}")
                .when().patch("/api/v1/inventory/999999/quantity")
                .then()
                .statusCode(404);
    }

    @Test
    public void testV1UpdateQuantityNegative() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"quantity\": -1}")
                .when().patch("/api/v1/inventory/329199/quantity")
                .then()
                .statusCode(400);
    }

    // ==================== DELETE /api/v1/inventory/{id} Tests ====================

    @Test
    public void testV1DeleteInventory() {
        // First create an item to delete
        int createdId = given()
                .contentType(ContentType.JSON)
                .body("{\"productId\": 6001, \"quantity\": 50}")
                .when().post("/api/v1/inventory")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Then delete it
        given()
                .when().delete("/api/v1/inventory/" + createdId)
                .then()
                .statusCode(204);

        // Verify it's deleted
        given()
                .when().get("/api/v1/inventory/" + createdId)
                .then()
                .statusCode(404);
    }

    @Test
    public void testV1DeleteInventoryNotFound() {
        given()
                .when().delete("/api/v1/inventory/999999")
                .then()
                .statusCode(404);
    }

    // ==================== Metrics Endpoint Tests ====================

    @Test
    public void testMetricsEndpoint() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200);
    }

    @Test
    public void testMetricsHasInventoryCount() {
        // First make a request to trigger metrics
        given().when().get("/api/v1/inventory").then().statusCode(200);

        // Check metrics endpoint contains our custom metrics
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("inventory"));
    }

    // ==================== Content-Type Tests ====================

    @Test
    public void testV1GetInventoryReturnsJson() {
        given()
                .when().get("/api/v1/inventory/329299")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    // ==================== Health Check Tests ====================

    @Test
    public void testV1HealthEndpoint() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200);
    }
}