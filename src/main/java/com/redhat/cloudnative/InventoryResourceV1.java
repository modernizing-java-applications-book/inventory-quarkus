package com.redhat.cloudnative;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.List;

/**
 * Inventory API v1 - Versioned endpoint with metrics and resilience patterns
 * 
 * API Versioning Strategy: URI Path versioning (/api/v1/inventory)
 */
@Path("/api/v1/inventory")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Inventory v1", description = "Inventory management operations (v1)")
public class InventoryResourceV1 {

    private static final Logger LOG = Logger.getLogger(InventoryResourceV1.class);

    @Inject
    @CacheName("inventory-cache")
    Cache inventoryCache;

    @Inject
    MeterRegistry meterRegistry;

    // ==================== GET ENDPOINTS (with metrics & resilience)
    // ====================

    @GET
    @Timeout(5000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000, successThreshold = 3)
    @Counted(value = "inventory.list.count", description = "How many times inventory list has been requested")
    @Timed(value = "inventory.list.timer", description = "Time taken to list inventory items", percentiles = { 0.5,
            0.95, 0.99 })
    @Operation(summary = "List all inventory items (v1)", description = "Returns a paginated list of inventory items with metadata")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Paginated list of inventory items", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PaginatedResponse.class))),
            @APIResponse(responseCode = "503", description = "Service unavailable - Circuit breaker open")
    })
    public PaginatedResponse<Inventory> listAll(
            @Parameter(description = "Page number (0-based)") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size (max 100)") @QueryParam("size") @DefaultValue("20") int size) {
        LOG.debugf("Listing inventory items - page: %d, size: %d", page, size);
        int effectiveSize = Math.min(size, 100);
        List<Inventory> items = Inventory.findAll()
                .page(page, effectiveSize)
                .list();
        long total = Inventory.count();
        // Record gauge metric
        meterRegistry.gauge("inventory.total.items", total);
        LOG.debugf("Found %d items out of %d total", items.size(), total);
        return PaginatedResponse.of(items, total, page, effectiveSize);
    }

    @GET
    @Path("/all")
    @Timeout(3000)
    @Counted(value = "inventory.list.all.count", description = "How many times all inventory has been requested")
    @Timed(value = "inventory.list.all.timer", description = "Time taken to list all inventory items")
    @Operation(summary = "List all inventory items without pagination (v1)", description = "Returns a simple list of all inventory items")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "List of all inventory items", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Inventory.class)))
    })
    public List<Inventory> listAllWithoutPagination() {
        LOG.debug("Listing all inventory items without pagination");
        return Inventory.listAll();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    @Counted(value = "inventory.count.requests", description = "How many times count has been requested")
    @Operation(summary = "Count inventory items (v1)", description = "Returns the total number of inventory items")
    @APIResponse(responseCode = "200", description = "Total count of inventory items")
    public Long count() {
        Long count = Inventory.count();
        LOG.debugf("Inventory count: %d", count);
        return count;
    }

    @GET
    @Path("/{itemId}")
    @Timeout(2000)
    @Retry(maxRetries = 3, delay = 100)
    @CacheResult(cacheName = "inventory-cache")
    @Counted(value = "inventory.get.by.id.count", description = "How many times get by ID has been requested")
    @Timed(value = "inventory.get.by.id.timer", description = "Time taken to get inventory by ID")
    @Operation(summary = "Get inventory by ID (v1)", description = "Returns a single inventory item by its ID (cached)")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Inventory item found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Inventory.class))),
            @APIResponse(responseCode = "404", description = "Inventory item not found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Inventory getAvailability(
            @Parameter(description = "Inventory item ID", required = true) @PathParam("itemId") Long itemId) {
        LOG.debugf("Getting inventory by ID: %d", itemId);
        Inventory inventory = Inventory.findById(itemId);
        if (inventory == null) {
            LOG.warnf("Inventory item not found with ID: %d", itemId);
            throw new InventoryNotFoundException(itemId);
        }
        return inventory;
    }

    @GET
    @Path("/product/{productId}")
    @Timeout(2000)
    @Retry(maxRetries = 3, delay = 100)
    @CacheResult(cacheName = "inventory-product-cache")
    @Counted(value = "inventory.get.by.product.count", description = "How many times get by product ID has been requested")
    @Timed(value = "inventory.get.by.product.timer", description = "Time taken to get inventory by product ID")
    @Operation(summary = "Get inventory by product ID (v1)", description = "Returns the inventory item for a specific product")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Inventory item found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Inventory.class))),
            @APIResponse(responseCode = "404", description = "Inventory item not found for the product", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Inventory getByProductId(
            @Parameter(description = "Product ID", required = true) @PathParam("productId") Long productId) {
        LOG.debugf("Getting inventory by product ID: %d", productId);
        Inventory inventory = Inventory.findByProductId(productId);
        if (inventory == null) {
            LOG.warnf("Inventory not found for product ID: %d", productId);
            throw new InventoryNotFoundException(productId);
        }
        return inventory;
    }

    // ==================== POST ENDPOINT ====================

    @POST
    @Transactional
    @Counted(value = "inventory.create.count", description = "How many inventory items have been created")
    @Timed(value = "inventory.create.timer", description = "Time taken to create inventory item")
    @Operation(summary = "Create inventory item (v1)", description = "Creates a new inventory item")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Inventory item created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Inventory.class))),
            @APIResponse(responseCode = "400", description = "Invalid inventory data", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @CacheInvalidateAll(cacheName = "inventory-cache")
    @CacheInvalidateAll(cacheName = "inventory-product-cache")
    public Response create(
            @RequestBody(description = "Inventory item to create", required = true, content = @Content(schema = @Schema(implementation = Inventory.class))) @Valid Inventory inventory) {
        LOG.infof("Creating inventory item for product ID: %d with quantity: %d", inventory.productId,
                inventory.quantity);
        inventory.id = null;
        inventory.persist();
        LOG.infof("Created inventory item with ID: %d", inventory.id);
        return Response.created(URI.create("/api/v1/inventory/" + inventory.id))
                .entity(inventory)
                .build();
    }

    // ==================== PUT ENDPOINT ====================

    @PUT
    @Path("/{itemId}")
    @Transactional
    @Counted(value = "inventory.update.count", description = "How many inventory items have been updated")
    @Timed(value = "inventory.update.timer", description = "Time taken to update inventory item")
    @Operation(summary = "Update inventory item (v1)", description = "Updates an existing inventory item completely")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Inventory item updated", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Inventory.class))),
            @APIResponse(responseCode = "400", description = "Invalid inventory data", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Inventory item not found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @CacheInvalidate(cacheName = "inventory-cache")
    @CacheInvalidateAll(cacheName = "inventory-product-cache")
    public Inventory update(
            @Parameter(description = "Inventory item ID", required = true) @PathParam("itemId") Long itemId,
            @RequestBody(description = "Updated inventory data", required = true, content = @Content(schema = @Schema(implementation = Inventory.class))) @Valid Inventory updatedInventory) {
        LOG.infof("Updating inventory item ID: %d with quantity: %d", itemId, updatedInventory.quantity);
        Inventory inventory = Inventory.findById(itemId);
        if (inventory == null) {
            LOG.warnf("Inventory item not found for update with ID: %d", itemId);
            throw new InventoryNotFoundException(itemId);
        }
        inventory.quantity = updatedInventory.quantity;
        inventory.persist();
        LOG.infof("Updated inventory item ID: %d", itemId);
        return inventory;
    }

    // ==================== PATCH ENDPOINT ====================

    @PATCH
    @Path("/{itemId}/quantity")
    @Transactional
    @Counted(value = "inventory.quantity.update.count", description = "How many quantity updates have been performed")
    @Timed(value = "inventory.quantity.update.timer", description = "Time taken to update quantity")
    @Operation(summary = "Update inventory quantity (v1)", description = "Updates only the quantity of an inventory item")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Quantity updated", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Inventory.class))),
            @APIResponse(responseCode = "400", description = "Invalid quantity value", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Inventory item not found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @CacheInvalidate(cacheName = "inventory-cache")
    @CacheInvalidateAll(cacheName = "inventory-product-cache")
    public Inventory updateQuantity(
            @Parameter(description = "Inventory item ID", required = true) @PathParam("itemId") Long itemId,
            @RequestBody(description = "New quantity value", required = true, content = @Content(schema = @Schema(implementation = QuantityUpdateRequest.class))) @Valid QuantityUpdateRequest request) {
        LOG.infof("Updating quantity for inventory ID: %d to %d", itemId, request.getQuantity());
        Inventory inventory = Inventory.findById(itemId);
        if (inventory == null) {
            LOG.warnf("Inventory item not found for quantity update with ID: %d", itemId);
            throw new InventoryNotFoundException(itemId);
        }
        inventory.quantity = request.getQuantity();
        inventory.persist();
        LOG.infof("Updated quantity for inventory ID: %d", itemId);
        return inventory;
    }

    // ==================== DELETE ENDPOINTS ====================

    @DELETE
    @Path("/{itemId}")
    @Transactional
    @Counted(value = "inventory.delete.count", description = "How many inventory items have been deleted")
    @Operation(summary = "Delete inventory item (v1)", description = "Deletes an inventory item by its ID")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Inventory item deleted"),
            @APIResponse(responseCode = "404", description = "Inventory item not found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @CacheInvalidate(cacheName = "inventory-cache")
    @CacheInvalidateAll(cacheName = "inventory-product-cache")
    public Response delete(
            @Parameter(description = "Inventory item ID", required = true) @PathParam("itemId") Long itemId) {
        LOG.infof("Deleting inventory item ID: %d", itemId);
        Inventory inventory = Inventory.findById(itemId);
        if (inventory == null) {
            LOG.warnf("Inventory item not found for deletion with ID: %d", itemId);
            throw new InventoryNotFoundException(itemId);
        }
        inventory.delete();
        LOG.infof("Deleted inventory item ID: %d", itemId);
        return Response.noContent().build();
    }

    /**
     * Clear all caches
     */
    @DELETE
    @Path("/cache")
    @Counted(value = "cache.clear.count", description = "How many times cache has been cleared")
    @Operation(summary = "Clear all inventory caches (v1)", description = "Clears all cached inventory data")
    @APIResponse(responseCode = "204", description = "Caches cleared")
    @CacheInvalidateAll(cacheName = "inventory-cache")
    @CacheInvalidateAll(cacheName = "inventory-product-cache")
    public Response clearCaches() {
        LOG.info("Clearing all inventory caches");
        return Response.noContent().build();
    }

}