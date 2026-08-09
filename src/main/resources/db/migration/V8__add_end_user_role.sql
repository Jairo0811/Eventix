IF NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE name = 'USER'
)
BEGIN
    INSERT INTO roles (name, description)
    VALUES ('USER', 'Usuario final de Eventix para descubrir eventos y gestionar sus boletas.');
END;
