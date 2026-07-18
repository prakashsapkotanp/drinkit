-- Connection requests (distinct from Follow) + direct messaging between connections

-- =========================
-- CONNECTIONS (request/accept/reject, mutual relationship once accepted)
-- =========================
CREATE TABLE connections (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requester_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    addressee_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status         VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT no_self_connection CHECK (requester_id <> addressee_id),
    CONSTRAINT unique_connection_pair UNIQUE (requester_id, addressee_id)
);

CREATE INDEX idx_connections_addressee ON connections (addressee_id, status);
CREATE INDEX idx_connections_requester ON connections (requester_id, status);

CREATE TRIGGER trg_connections_updated_at
    BEFORE UPDATE ON connections
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =========================
-- CONVERSATIONS (one per connected pair; canonical ordering prevents duplicates)
-- =========================
CREATE TABLE conversations (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_a_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_b_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_message_at TIMESTAMPTZ,
    CONSTRAINT ordered_pair CHECK (user_a_id < user_b_id),
    CONSTRAINT unique_conversation_pair UNIQUE (user_a_id, user_b_id)
);

CREATE INDEX idx_conversations_user_a ON conversations (user_a_id, last_message_at DESC);
CREATE INDEX idx_conversations_user_b ON conversations (user_b_id, last_message_at DESC);

-- =========================
-- MESSAGES
-- =========================
CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text            VARCHAR(2000) NOT NULL,
    read            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_id ON messages (conversation_id, created_at DESC);

CREATE OR REPLACE FUNCTION touch_conversation_last_message()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE conversations SET last_message_at = NEW.created_at WHERE id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_messages_touch_conversation
    AFTER INSERT ON messages
    FOR EACH ROW EXECUTE FUNCTION touch_conversation_last_message();
