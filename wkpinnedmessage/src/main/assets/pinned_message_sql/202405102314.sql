create table pinned_message
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id TEXT,
    message_seq BIGINT default 0,
    channel_id TEXT,
    channel_type int default 0,
    is_deleted int default 0,
    `version` bigint default 0,
    created_at text,
    updated_at text
);
CREATE UNIQUE INDEX IF NOT EXISTS pinned_message_message_idx ON pinned_message (message_id);
CREATE INDEX IF NOT EXISTS pinned_message_channel_idx ON pinned_message (channel_id, channel_type);
