-- Create profile table
CREATE TABLE IF NOT EXISTS user_profile
(
    user_id   UUID PRIMARY KEY,

    full_name TEXT NOT NULL DEFAULT '',

    CONSTRAINT fk_user_profile_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);