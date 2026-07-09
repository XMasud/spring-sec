CREATE TABLE users
(
    id                    UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(255),
    roles                 VARCHAR(255) NOT NULL DEFAULT 'USER',
    enabled               BOOLEAN      NOT NULL DEFAULT true,
    account_locked        BOOLEAN      NOT NULL DEFAULT false,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    created_at            TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users (email);