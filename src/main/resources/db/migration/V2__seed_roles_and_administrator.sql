INSERT INTO roles (id, name, description) VALUES
    (1, 'ADMINISTRATOR', 'Acceso completo a la administración de Eventix.'),
    (2, 'OPERATOR', 'Operación de clientes, reservaciones, ventas y pagos.'),
    (3, 'ORGANIZER', 'Gestión y consulta de eventos propios.'),
    (4, 'ACCESS_STAFF', 'Validación de boletas y control de acceso.');

INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    username,
    password_hash,
    phone,
    role_id,
    status,
    must_change_password,
    last_login_at,
    created_at,
    updated_at,
    created_by,
    updated_by,
    version
) VALUES (
    1,
    'Administrador',
    'Eventix',
    'admin@eventix.local',
    'admin',
    '$2b$12$LKYb4TTYExP1wmxhLqfF0.g.rol1KiBx0qm86U3Bp/MvCR3lI59Am',
    NULL,
    1,
    'ACTIVE',
    TRUE,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'flyway',
    'flyway',
    0
);
