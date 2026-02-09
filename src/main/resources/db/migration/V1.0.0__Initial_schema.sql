-- Initial inventory schema for PostgreSQL
-- Flyway migration script

CREATE TABLE IF NOT EXISTS INVENTORY (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index on product_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON INVENTORY(product_id);

-- Create index on quantity for low-stock queries
CREATE INDEX IF NOT EXISTS idx_inventory_quantity ON INVENTORY(quantity);

-- Add constraint for non-negative quantity
ALTER TABLE INVENTORY ADD CONSTRAINT chk_quantity_non_negative CHECK (quantity >= 0);

-- Insert initial data
INSERT INTO INVENTORY (product_id, quantity, created_at, updated_at) VALUES
    (1001, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1002, 35, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1003, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1004, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1005, 25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1007, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1008, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1009, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
+++++++ REPLACE