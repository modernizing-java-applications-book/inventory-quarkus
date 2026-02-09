package com.redhat.cloudnative;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class InventoryResourceTest {

        // ==================== GET /api/inventory Tests (Paginated)
        // ====================

        @Test
        public void testListAllInventoryDefaultPagination() {
                given()
                                .when().get("/api/inventory")
                                .then()
                                .statusCode(200)
                                .body("page", is(0))
                                .body("size", is(20))
                                .body("hasNext", is(false))
                                .body("hasPrevious", is(false));
        }

        @Test
        public void testListInventoryWithPagination() {
                given()
                                .queryParam("page", 0)
                                .queryParam("size", 3)
                                .when().get("/api/inventory")
                                .then()
                                .statusCode(200)
                                .body("data.size()", is(3))
                                .body("page", is(0))
                                .body("size", is(3))
                                .body("hasNext", is(true))
                                .body("hasPrevious", is(false));
        }

        @Test
        public void testListInventoryWithPaginationSecondPage() {
                given()
                                .queryParam("page", 1)
                                .queryParam("size", 3)
                                .when().get("/api/inventory")
                                .then()
                                .statusCode(200)
                                .body("data.size()", is(3))
                                .body("page", is(1))
                                .body("size", is(3))
                                .body("hasNext", is(true))
                                .body("hasPrevious", is(true));
        }

        @Test
        public void testListInventoryLastPage() {
                given()
                                .queryParam("page", 2)
                                .queryParam("size", 3)
                                .when().get("/api/inventory")
                                .then()
                                .statusCode(200)
                                .body("page", is(2))
                                .body("size", is(3))
                                .body("hasPrevious", is(true));
        }

        @Test
        public void testListInventoryMaxSizeLimit() {
                given()
                                .queryParam("page", 0)
                                .queryParam("size", 200)
                                .when().get("/api/inventory")
                                .then()
                                .statusCode(200)
                                .body("size", is(100)); // Should be limited to 100
        }

        // ==================== GET /api/inventory/all Tests (No Pagination)
        // ====================

        @Test
        public void testListAllWithoutPagination() {
                given()
                                .when().get("/api/inventory/all")
                                .then()
                                .statusCode(200);
        }

        // ==================== GET /api/inventory/count Tests ====================

        @Test
        public void testCountInventory() {
                given()
                                .when().get("/api/inventory/count")
                                .then()
                                .statusCode(200);
        }

        // ==================== GET /api/inventory/{id} Tests ====================

        @Test
        public void testGetInventoryById() {
                given()
                                .when().get("/api/inventory/329299")
                                .then()
                                .statusCode(200)
                                .body("id", is(329299))
                                .body("quantity", is(35));
        }

        @Test
        public void testGetInventoryByIdNotFound() {
                given()
                                .when().get("/api/inventory/999999")
                                .then()
                                .statusCode(404)
                                .body("status", is(404))
                                .body("error", is("Not Found"))
                                .body("message", containsString("not found"));
        }

        @Test
        public void testGetInventoryByIdExistingItem() {
                given()
                                .when().get("/api/inventory/100000")
                                .then()
                                .statusCode(200)
                                .body("id", is(100000))
                                .body("quantity", is(0));
        }

        // ==================== POST /api/inventory Tests ====================

        @Test
        public void testCreateInventory() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"productId\": 2001, \"quantity\": 100}")
                                .when().post("/api/inventory")
                                .then()
                                .statusCode(201)
                                .body("quantity", is(100))
                                .body("productId", is(2001))
                                .header("Location", containsString("/api/inventory/"));
        }

        @Test
        public void testCreateInventoryWithZeroQuantity() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"productId\": 2002, \"quantity\": 0}")
                                .when().post("/api/inventory")
                                .then()
                                .statusCode(201)
                                .body("quantity", is(0))
                                .body("productId", is(2002));
        }

        @Test
        public void testCreateInventoryWithNegativeQuantity() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"productId\": 2003, \"quantity\": -10}")
                                .when().post("/api/inventory")
                                .then()
                                .statusCode(400);
        }

        @Test
        public void testCreateInventoryWithoutProductId() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"quantity\": 50}")
                                .when().post("/api/inventory")
                                .then()
                                .statusCode(400);
        }

        // ==================== PUT /api/inventory/{id} Tests ====================

        @Test
        public void testUpdateInventory() {
                // Use a different ID (165614) to avoid interfering with other tests
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"productId\": 1005, \"quantity\": 999}")
                                .when().put("/api/inventory/165614")
                                .then()
                                .statusCode(200)
                                .body("id", is(165614))
                                .body("quantity", is(999));
        }

        @Test
        public void testUpdateInventoryNotFound() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"productId\": 9999, \"quantity\": 100}")
                                .when().put("/api/inventory/999999")
                                .then()
                                .statusCode(404)
                                .body("status", is(404));
        }

        @Test
        public void testUpdateInventoryWithNegativeQuantity() {
                // Use a different ID (165954) to avoid interfering with other tests
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"productId\": 1006, \"quantity\": -5}")
                                .when().put("/api/inventory/165954")
                                .then()
                                .statusCode(400);
        }

        // ==================== PATCH /api/inventory/{id}/quantity Tests
        // ====================

        @Test
        public void testUpdateQuantity() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"quantity\": 500}")
                                .when().patch("/api/inventory/329199/quantity")
                                .then()
                                .statusCode(200)
                                .body("id", is(329199))
                                .body("quantity", is(500));
        }

        @Test
        public void testUpdateQuantityMissingParameter() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{}")
                                .when().patch("/api/inventory/329199/quantity")
                                .then()
                                .statusCode(400)
                                .body("status", is(400))
                                .body("error", containsString("Validation"));
        }

        @Test
        public void testUpdateQuantityNegative() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"quantity\": -1}")
                                .when().patch("/api/inventory/329199/quantity")
                                .then()
                                .statusCode(400)
                                .body("status", is(400))
                                .body("message", containsString("negative"));
        }

        @Test
        public void testUpdateQuantityNotFound() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{\"quantity\": 100}")
                                .when().patch("/api/inventory/999999/quantity")
                                .then()
                                .statusCode(404);
        }

        // ==================== DELETE /api/inventory/{id} Tests ====================

        @Test
        public void testDeleteInventory() {
                // First create an item to delete
                int createdId = given()
                                .contentType(ContentType.JSON)
                                .body("{\"productId\": 3001, \"quantity\": 50}")
                                .when().post("/api/inventory")
                                .then()
                                .statusCode(201)
                                .extract().path("id");

                // Then delete it
                given()
                                .when().delete("/api/inventory/" + createdId)
                                .then()
                                .statusCode(204);

                // Verify it's deleted
                given()
                                .when().get("/api/inventory/" + createdId)
                                .then()
                                .statusCode(404);
        }

        // ==================== GET /api/inventory/product/{productId} Tests
        // ====================

        @Test
        public void testGetInventoryByProductId() {
                given()
                                .when().get("/api/inventory/product/1002")
                                .then()
                                .statusCode(200)
                                .body("productId", is(1002))
                                .body("quantity", is(35));
        }

        @Test
        public void testGetInventoryByProductIdNotFound() {
                given()
                                .when().get("/api/inventory/product/999999")
                                .then()
                                .statusCode(404)
                                .body("status", is(404))
                                .body("error", is("Not Found"));
        }

        @Test
        public void testDeleteInventoryNotFound() {
                given()
                                .when().delete("/api/inventory/999999")
                                .then()
                                .statusCode(404);
        }

        // ==================== Content-Type and Headers Tests ====================

        @Test
        public void testGetInventoryReturnsJson() {
                given()
                                .when().get("/api/inventory/329299")
                                .then()
                                .statusCode(200)
                                .contentType(ContentType.JSON);
        }

        @Test
        public void testCreateInventoryWithWrongContentType() {
                given()
                                .contentType(ContentType.TEXT)
                                .body("{\"quantity\": 100}")
                                .when().post("/api/inventory")
                                .then()
                                .statusCode(415); // Unsupported Media Type
        }

        // ==================== Health Check Tests ====================

        @Test
        public void testHealthEndpoint() {
                given()
                                .when().get("/q/health")
                                .then()
                                .statusCode(200);
        }

        @Test
        public void testHealthReadyEndpoint() {
                given()
                                .when().get("/q/health/ready")
                                .then()
                                .statusCode(200);
        }

        @Test
        public void testHealthLiveEndpoint() {
                given()
                                .when().get("/q/health/live")
                                .then()
                                .statusCode(200);
        }
}