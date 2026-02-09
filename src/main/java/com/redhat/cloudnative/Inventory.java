package com.redhat.cloudnative;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Entity
@Table(name = "INVENTORY")
public class Inventory extends PanacheEntity {

    @Column(name = "product_id", unique = true)
    @NotNull(message = "Product ID is required")
    @Schema(description = "Associated product ID", required = true, example = "1001")
    public Long productId;

    @Column(name = "quantity")
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Schema(description = "Current stock quantity", required = true, example = "50")
    public int quantity;

    @Column(name = "created_at", updatable = false)
    @Schema(description = "Creation timestamp", readOnly = true)
    public Instant createdAt;

    @Column(name = "updated_at")
    @Schema(description = "Last update timestamp", readOnly = true)
    public Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "Inventory [Id='" + id + '\'' + ", productId=" + productId + ", quantity=" + quantity +
                ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ']';
    }

    /**
     * Get the ID (for OpenAPI documentation)
     */
    @Schema(description = "Inventory ID (auto-generated, read-only)", readOnly = true)
    public Long getId() {
        return id;
    }

    /**
     * Find inventory by product ID
     */
    public static Inventory findByProductId(Long productId) {
        return find("productId", productId).firstResult();
    }

    /**
     * Check if inventory exists for a product
     */
    public static boolean existsByProductId(Long productId) {
        return count("productId", productId) > 0;
    }
}