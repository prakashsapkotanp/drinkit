-- V2__post_counters_triggers.sql
-- Keep posts.like_count and posts.comment_count in sync automatically

CREATE OR REPLACE FUNCTION increment_like_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE posts SET like_count = like_count + 1 WHERE id = NEW.post_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION decrement_like_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE posts SET like_count = GREATEST(like_count - 1, 0) WHERE id = OLD.post_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_likes_insert
    AFTER INSERT ON likes
    FOR EACH ROW EXECUTE FUNCTION increment_like_count();

CREATE TRIGGER trg_likes_delete
    AFTER DELETE ON likes
    FOR EACH ROW EXECUTE FUNCTION decrement_like_count();

CREATE OR REPLACE FUNCTION increment_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE posts SET comment_count = comment_count + 1 WHERE id = NEW.post_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION decrement_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE posts SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = OLD.post_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comments_insert
    AFTER INSERT ON comments
    FOR EACH ROW EXECUTE FUNCTION increment_comment_count();

CREATE TRIGGER trg_comments_delete
    AFTER DELETE ON comments
    FOR EACH ROW EXECUTE FUNCTION decrement_comment_count();
