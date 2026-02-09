package com.redhat.cloudnative;

public class InventoryNotFoundException extends RuntimeException {

    private final Long itemId;

    public InventoryNotFoundException(Long itemId) {
        super("Inventory item not found with id: " + itemId);
        this.itemId = itemId;
    }

    public Long getItemId() {
        return itemId;
    }
}