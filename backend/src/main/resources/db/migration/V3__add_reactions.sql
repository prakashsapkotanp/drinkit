-- V3__add_reactions.sql
-- Replaces simple likes with a multi-reaction system + audit trail

-- =========================
-- REACTION TYPES (catalog)
-- =========================
CREATE TABLE reaction_types (
    id          SMALLINT PRIMARY KEY,
    code        VARCHAR(20) NOT NULL UNIQUE,   -- LIKE, LOVE, CHEERS, WOW, SAD
    label       VARCHAR(50) NOT NULL,
    emoji       VARCHAR(10)
);

INSERT INTO reaction_types (id, code, label, emoji) VALUES
    (1, 'LIKE',   'Like',   '👍'),
    (2, 'LOVE',   'Love',   '❤️'),
    (3, 'CHEERS', 'Cheers', '🥂'),
    (4, 'WOW',    'Wow',    '😮'),
    (5, 'SAD',    'Sad',    '😢');

-- =========================
-- POST REACTIONS (current state — one reaction per user per post)
-- =========================
CREATE TABLE post_reactions (
    post_id          UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reaction_type_id SMALLINT NOT NULL REFERENCES reaction_types (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE INDEX idx_post_reactions_post_id ON post_reactions (post_id);
CREATE INDEX idx_post_reactions_type ON post_reactions (post_id, reaction_type_id);

-- =========================
-- REACTION TRANSACTIONS (append-only audit log)
-- =========================
CREATE TABLE reaction_transactions (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    post_id                UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id                UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    action                 VARCHAR(10) NOT NULL CHECK (action IN ('ADDED', 'CHANGED', 'REMOVED')),
    reaction_type_id       SMALLINT REFERENCES reaction_types (id),      -- new value (null if REMOVED)
    previous_reaction_type_id SMALLINT REFERENCES reaction_types (id),   -- old value (null if ADDED)
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reaction_tx_post_id ON reaction_transactions (post_id, created_at DESC);
CREATE INDEX idx_reaction_tx_user_id ON reaction_transactions (user_id, created_at DESC);

-- =========================
-- Per-type reaction counts on posts (replaces single like_count)
-- =========================
ALTER TABLE posts DROP COLUMN IF EXISTS like_count;
ALTER TABLE posts ADD COLUMN reaction_counts JSONB NOT NULL DEFAULT '{}';
-- e.g. {"LIKE": 12, "LOVE": 4, "CHEERS": 1}

-- =========================
-- Migrate existing simple likes into the new model, then drop old table
-- =========================
INSERT INTO post_reactions (post_id, user_id, reaction_type_id, created_at, updated_at)
SELECT post_id, user_id, 1, created_at, created_at FROM likes
ON CONFLICT DO NOTHING;

INSERT INTO reaction_transactions (post_id, user_id, action, reaction_type_id, created_at)
SELECT post_id, user_id, 'ADDED', 1, created_at FROM likes;

DROP TABLE IF EXISTS likes;
