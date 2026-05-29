-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    login VARCHAR(64) PRIMARY KEY,
    password_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица образцов
CREATE TABLE IF NOT EXISTS samples (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64) NOT NULL,
    location VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    owner_username VARCHAR(64) NOT NULL REFERENCES users(login) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- Таблица измерений
CREATE TABLE IF NOT EXISTS measurements (
    id BIGSERIAL PRIMARY KEY,
    sample_id BIGINT NOT NULL REFERENCES samples(id) ON DELETE CASCADE,
    param VARCHAR(32) NOT NULL CHECK (param IN ('PH', 'CONDUCTIVITY', 'TURBIDITY', 'NITRATE')),
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(16) NOT NULL,
    method VARCHAR(64) NOT NULL,
    measured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    owner_username VARCHAR(64) NOT NULL REFERENCES users(login) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- Таблица протоколов
CREATE TABLE IF NOT EXISTS protocols (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    owner_username VARCHAR(64) NOT NULL REFERENCES users(login) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица параметров протокола
CREATE TABLE IF NOT EXISTS protocol_params (
    protocol_id BIGINT NOT NULL REFERENCES protocols(id) ON DELETE CASCADE,
    param VARCHAR(32) NOT NULL CHECK (param IN ('PH', 'CONDUCTIVITY', 'TURBIDITY', 'NITRATE')),
    PRIMARY KEY (protocol_id, param)
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_samples_owner ON samples(owner_username);
CREATE INDEX IF NOT EXISTS idx_samples_status ON samples(status);
CREATE INDEX IF NOT EXISTS idx_measurements_sample ON measurements(sample_id);
CREATE INDEX IF NOT EXISTS idx_measurements_param ON measurements(param);
CREATE INDEX IF NOT EXISTS idx_protocols_owner ON protocols(owner_username);