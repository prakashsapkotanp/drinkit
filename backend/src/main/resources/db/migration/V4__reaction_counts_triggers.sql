-- V4__reaction_counts_triggers.sql
-- Maintain reaction_counts JSONB automatically on post_reactions change

CREATE OR REPLACE FUNCTION adjust_reaction_count(p_post_id UUID, p_type_id SMALLINT, p_delta INT)
RETURNS VOID AS $$
DECLARE
    v_code TEXT;
BEGIN
    SELECT code INTO v_code FROM reaction_types WHERE id = p_type_id;
    UPDATE posts
    SET reaction_counts = jsonb_set(
        reaction_counts,
        ARRAY[v_code],
        to_jsonb(GREATEST(COALESCE((reaction_counts->>v_code)::int, 0) + p_delta, 0))
    )
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION post_reactions_insert_trigger()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM adjust_reaction_count(NEW.post_id, NEW.reaction_type_id, 1);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION post_reactions_update_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.reaction_type_id <> OLD.reaction_type_id THEN
        PERFORM adjust_reaction_count(OLD.post_id, OLD.reaction_type_id, -1);
        PERFORM adjust_reaction_count(NEW.post_id, NEW.reaction_type_id, 1);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION post_reactions_delete_trigger()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM adjust_reaction_count(OLD.post_id, OLD.reaction_type_id, -1);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_post_reactions_insert
    AFTER INSERT ON post_reactions
    FOR EACH ROW EXECUTE FUNCTION post_reactions_insert_trigger();

CREATE TRIGGER trg_post_reactions_update
    AFTER UPDATE ON post_reactions
    FOR EACH ROW EXECUTE FUNCTION post_reactions_update_trigger();

CREATE TRIGGER trg_post_reactions_delete
    AFTER DELETE ON post_reactions
    FOR EACH ROW EXECUTE FUNCTION post_reactions_delete_trigger();
