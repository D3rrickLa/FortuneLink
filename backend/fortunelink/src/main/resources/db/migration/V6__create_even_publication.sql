-- -- For Spring Modulith
CREATE TABLE IF NOT EXISTS event_publication (
    id                      UUID        NOT NULL,
    listener_id             TEXT        NOT NULL,
    event_type              TEXT        NOT NULL,
    serialized_event        TEXT        NOT NULL,
    publication_date        TIMESTAMPTZ NOT NULL,
    completion_date         TIMESTAMPTZ,
    completion_attempts     INT       NOT NULL,
    last_resubmission_date  TIMESTAMPTZ,
    status                  VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_event_publication_completion_date
    ON event_publication (completion_date);

CREATE INDEX IF NOT EXISTS idx_event_publication_listener_id
    ON event_publication (listener_id);