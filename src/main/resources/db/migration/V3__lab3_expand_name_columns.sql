ALTER TABLE customers
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN last_name VARCHAR(255);

CREATE FUNCTION sync_customer_name_columns()
RETURNS TRIGGER AS $$
DECLARE
    normalized_name TEXT;
    separator_position INTEGER;
BEGIN
    IF TG_OP = 'INSERT' AND NEW.full_name IS NULL THEN
        NEW.full_name := concat_ws(' ', NEW.first_name, NULLIF(NEW.last_name, ''));
        RETURN NEW;
    END IF;

    IF TG_OP = 'UPDATE'
        AND NEW.full_name IS NOT DISTINCT FROM OLD.full_name
        AND (NEW.first_name IS DISTINCT FROM OLD.first_name
            OR NEW.last_name IS DISTINCT FROM OLD.last_name) THEN
        NEW.full_name := concat_ws(' ', NEW.first_name, NULLIF(NEW.last_name, ''));
        RETURN NEW;
    END IF;

    normalized_name := regexp_replace(btrim(NEW.full_name), '\s+', ' ', 'g');
    separator_position := position(' ' IN normalized_name);
    NEW.first_name := CASE
        WHEN separator_position = 0 THEN normalized_name
        ELSE substring(normalized_name FROM 1 FOR separator_position - 1)
    END;
    NEW.last_name := CASE
        WHEN separator_position = 0 THEN ''
        ELSE substring(normalized_name FROM separator_position + 1)
    END;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER customers_name_sync
BEFORE INSERT OR UPDATE ON customers
FOR EACH ROW
EXECUTE FUNCTION sync_customer_name_columns();
