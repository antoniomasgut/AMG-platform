CREATE TABLE IF NOT EXISTS model_pricing (
    model_name              VARCHAR(100) PRIMARY KEY,
    provider                VARCHAR(50)  NOT NULL,
    input_cost_micros       BIGINT       NOT NULL DEFAULT 0,
    output_cost_micros      BIGINT       NOT NULL DEFAULT 0,
    markup_percent          INTEGER      NOT NULL DEFAULT 20,
    client_input_micros     BIGINT       NOT NULL DEFAULT 0,
    client_output_micros    BIGINT       NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
