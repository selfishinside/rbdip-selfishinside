CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50)
);

INSERT INTO customers (full_name, address, phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone
FROM orders;

ALTER TABLE orders ADD COLUMN customer_id BIGINT;

UPDATE orders AS orders_to_update
SET customer_id = customers.id
FROM customers
WHERE customers.full_name = orders_to_update.customer_full_name
  AND customers.address IS NOT DISTINCT FROM orders_to_update.customer_address
  AND customers.phone IS NOT DISTINCT FROM orders_to_update.customer_phone;

ALTER TABLE orders ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);

ALTER TABLE orders
    DROP COLUMN customer_full_name,
    DROP COLUMN customer_address,
    DROP COLUMN customer_phone;

ALTER TABLE order_items ADD COLUMN product_id BIGINT;

UPDATE order_items AS items_to_update
SET product_id = (
    SELECT products.id
    FROM products
    WHERE products.name = items_to_update.product_name
      AND products.price = items_to_update.product_price
    ORDER BY products.id
    LIMIT 1
);

ALTER TABLE order_items ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products(id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

ALTER TABLE order_items
    DROP COLUMN product_name,
    DROP COLUMN product_price;
