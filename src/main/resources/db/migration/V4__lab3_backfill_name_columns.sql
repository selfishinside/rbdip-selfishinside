UPDATE customers
SET first_name = CASE
        WHEN position(' ' IN regexp_replace(btrim(full_name), '\s+', ' ', 'g')) = 0
            THEN regexp_replace(btrim(full_name), '\s+', ' ', 'g')
        ELSE substring(
            regexp_replace(btrim(full_name), '\s+', ' ', 'g')
            FROM 1
            FOR position(' ' IN regexp_replace(btrim(full_name), '\s+', ' ', 'g')) - 1)
    END,
    last_name = CASE
        WHEN position(' ' IN regexp_replace(btrim(full_name), '\s+', ' ', 'g')) = 0 THEN ''
        ELSE substring(
            regexp_replace(btrim(full_name), '\s+', ' ', 'g')
            FROM position(' ' IN regexp_replace(btrim(full_name), '\s+', ' ', 'g')) + 1)
    END;

ALTER TABLE customers
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL;
