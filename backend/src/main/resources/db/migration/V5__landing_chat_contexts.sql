CREATE TABLE landing_chat_contexts (
    landing_id          UUID PRIMARY KEY REFERENCES landings(id) ON DELETE CASCADE,
    business_name       VARCHAR(200) NOT NULL,
    sector              VARCHAR(50),
    system_prompt       TEXT NOT NULL,
    profanity_action    VARCHAR(20) NOT NULL DEFAULT 'CLOSE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
