DROP TRIGGER customers_name_sync ON customers;
DROP FUNCTION sync_customer_name_columns();
ALTER TABLE customers DROP COLUMN full_name;
