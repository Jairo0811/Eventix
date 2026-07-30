/* ================================================================
   EVENTIX
   V2 - Roles y administrador inicial

   Usuario: admin
   Correo: admin@eventix.local
   Contraseña temporal: Admin123*
   ================================================================ */

INSERT INTO roles
(
    name,
    description
)
VALUES
(
    'ADMINISTRATOR',
    'Acceso completo a la administración de Eventix.'
);

INSERT INTO roles
(
    name,
    description
)
VALUES
(
    'OPERATOR',
    'Operación de clientes, reservaciones, ventas y pagos.'
);

INSERT INTO roles
(
    name,
    description
)
VALUES
(
    'ORGANIZER',
    'Gestión y consulta de eventos propios.'
);

INSERT INTO roles
(
    name,
    description
)
VALUES
(
    'ACCESS_STAFF',
    'Validación de boletas y control de acceso.'
);

INSERT INTO users
(
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
)
SELECT
    'Administrador',
    'Eventix',
    'admin@eventix.local',
    'admin',
    '$2b$12$LKYb4TTYExP1wmxhLqfF0.g.rol1KiBx0qm86U3Bp/MvCR3lI59Am',
    NULL,
    id,
    'ACTIVE',
    1,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'flyway',
    'flyway',
    0
FROM roles
WHERE name = 'ADMINISTRATOR';