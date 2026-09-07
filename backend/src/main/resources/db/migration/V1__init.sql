-- Ewidencja zwrotów – schemat początkowy (UTF-8)

CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_setting (
    key             VARCHAR(100) PRIMARY KEY,
    value           TEXT NOT NULL,
    type            VARCHAR(20) NOT NULL,
    label           VARCHAR(200) NOT NULL,
    description     TEXT
);

INSERT INTO app_setting (key, value, type, label, description) VALUES
    ('SYNC_INTERVAL_MINUTES', '15', 'INTEGER', 'Interwał synchronizacji Allegro (minuty)', 'Jak często pobierać nowe zwroty z Allegro. Zalecane minimum 5 minut, aby szanować limit 1 żądania na sekundę.'),
    ('VAT_RATE_PERCENT', '23', 'DECIMAL', 'Domyślna stawka VAT (%)', 'Używana przez przycisk „Wypełnij VAT” na ewidencji miesiąca.'),
    ('APILO_LIMIT_PER_MINUTE', '200', 'INTEGER', 'Limit zapytań Apilo (req/min)', 'Limit z abonamentu Apilo. Domyślnie 200/min.'),
    ('APILO_LIMIT_MARGIN_PERCENT', '80', 'INTEGER', 'Margines limitu Apilo (%)', 'Rzeczywisty limit = limit × margines / 100. Domyślnie 80% z 200 = 160 req/min.'),
    ('APILO_PREFILL_BACKGROUND', 'true', 'BOOLEAN', 'Prefill numeru Apilo w tle', 'Po pobraniu zwrotu z Allegro dociąga numer zamówienia Apilo w tle (przez limiter).'),
    ('TIMEZONE', 'Europe/Warsaw', 'STRING', 'Strefa czasowa', 'Strefa używana przy datach ewidencji i synchronizacji.'),
    ('USER_AGENT', 'Returns-Ewidencja/1.0.0', 'STRING', 'User-Agent HTTP', 'Identyfikacja oprogramowania w żądaniach do Allegro (wymóg regulaminu REST API).');

CREATE TABLE allegro_account (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(200) NOT NULL,
    client_id            VARCHAR(255) NOT NULL,
    client_secret_enc    TEXT,
    access_token_enc     TEXT,
    refresh_token_enc    TEXT,
    token_expires_at     TIMESTAMPTZ,
    allegro_login        VARCHAR(200),
    sandbox              BOOLEAN NOT NULL DEFAULT FALSE,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    last_sync_at         TIMESTAMPTZ,
    last_error           TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE apilo_connection (
    id                   SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    base_url             VARCHAR(500) NOT NULL DEFAULT 'https://api.apilo.com',
    client_id            VARCHAR(255),
    client_secret_enc    TEXT,
    access_token_enc     TEXT,
    refresh_token_enc    TEXT,
    access_expires_at    TIMESTAMPTZ,
    refresh_expires_at   TIMESTAMPTZ,
    last_error           TEXT,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO apilo_connection (id) VALUES (1);

CREATE TABLE allegro_return (
    id                   UUID PRIMARY KEY,
    account_id           BIGINT NOT NULL REFERENCES allegro_account (id) ON DELETE CASCADE,
    reference_number     VARCHAR(100),
    order_id             VARCHAR(100),
    created_at           TIMESTAMPTZ,
    status               VARCHAR(50),
    buyer_login          VARCHAR(200),
    buyer_email          VARCHAR(300),
    items                JSONB NOT NULL DEFAULT '[]'::jsonb,
    parcels              JSONB NOT NULL DEFAULT '[]'::jsonb,
    raw                  JSONB,
    local_state          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    apilo_order_number   VARCHAR(50),
    first_seen_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT allegro_return_state_chk CHECK (local_state IN ('NEW', 'RECOGNIZED', 'IGNORED'))
);

CREATE INDEX idx_allegro_return_account_state ON allegro_return (account_id, local_state);
CREATE INDEX idx_allegro_return_created ON allegro_return (created_at DESC);

CREATE TABLE return_record (
    id                   BIGSERIAL PRIMARY KEY,
    allegro_return_id    UUID UNIQUE REFERENCES allegro_return (id) ON DELETE SET NULL,
    account_id           BIGINT REFERENCES allegro_account (id) ON DELETE SET NULL,
    apilo_order_number   VARCHAR(50),
    sale_date            DATE,
    buyer_name           VARCHAR(300),
    product_codes        TEXT,
    return_date          DATE NOT NULL,
    gross_amount         NUMERIC(12, 2) NOT NULL,
    currency             VARCHAR(8) NOT NULL DEFAULT 'PLN',
    amount_estimated     BOOLEAN NOT NULL DEFAULT FALSE,
    vat_amount           NUMERIC(12, 2),
    notes                TEXT,
    period               DATE NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_return_record_period ON return_record (period, id);

CREATE TABLE sync_run (
    id                   BIGSERIAL PRIMARY KEY,
    account_id           BIGINT REFERENCES allegro_account (id) ON DELETE CASCADE,
    started_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at          TIMESTAMPTZ,
    fetched              INTEGER,
    new_count            INTEGER,
    error                TEXT
);
