-- V1__init_core_schema.sql
-- Core schema: users, posts, follows, likes, comments, notifications

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================
-- USERS
-- =========================
CREATE TABLE users (
id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
email             VARCHAR(255) NOT NULL UNIQUE,
username          VARCHAR(50)  NOT NULL UNIQUE,
password_hash     VARCHAR(255) NOT NULL,
display_name      VARCHAR(100),
bio               VARCHAR(500),
avatar_url        TEXT,
date_of_birth     DATE NOT NULL,
age_verified      BOOLEAN NOT NULL DEFAULT FALSE,
drink_preferences TEXT[] DEFAULT '{}',
created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);

-- =========================
-- POSTS
-- =========================
CREATE TABLE posts (
id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
author_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
text           VARCHAR(3000) NOT NULL,
drink_category VARCHAR(20) NOT NULL CHECK (drink_category IN ('ALCOHOLIC', 'NON_ALCOHOLIC')),
drink_type     VARCHAR(50),        -- e.g. wine, beer, coffee, tea, cocktail, spirit
rating         SMALLINT CHECK (rating BETWEEN 1 AND 5),
tasting_notes  VARCHAR(1000),
scenario       VARCHAR(100),       -- e.g. "with friends", "morning routine"
media_urls     TEXT[] DEFAULT '{}',
like_count     INTEGER NOT NULL DEFAULT 0,
comment_count  INTEGER NOT NULL DEFAULT 0,
created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_author_id ON posts (author_id);
CREATE INDEX idx_posts_created_at ON posts (created_at DESC);
CREATE INDEX idx_posts_drink_category ON posts (drink_category);

-- =========================
-- FOLLOWS (social graph)
-- =========================
CREATE TABLE follows (
follower_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
following_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
PRIMARY KEY (follower_id, following_id),
CONSTRAINT no_self_follow CHECK (follower_id <> following_id)
);

CREATE INDEX idx_follows_following_id ON follows (following_id);

-- =========================
-- LIKES
-- =========================
CREATE TABLE likes (
user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
post_id    UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
PRIMARY KEY (user_id, post_id)
);

CREATE INDEX idx_likes_post_id ON likes (post_id);

-- =========================
-- COMMENTS
-- =========================
CREATE TABLE comments (
id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
post_id    UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
author_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
text       VARCHAR(1000) NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_post_id ON comments (post_id, created_at);

-- =========================
-- NOTIFICATIONS
-- =========================
CREATE TABLE notifications (
id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
type            VARCHAR(20) NOT NULL CHECK (type IN ('LIKE', 'COMMENT', 'FOLLOW', 'MENTION')),
source_user_id  UUID REFERENCES users (id) ON DELETE SET NULL,
reference_id    UUID,  -- post_id or comment_id depending on type
read            BOOLEAN NOT NULL DEFAULT FALSE,
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id, read, created_at DESC);

-- =========================
-- TRIGGERS: keep updated_at fresh
-- =========================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_posts_updated_at
BEFORE UPDATE ON posts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();