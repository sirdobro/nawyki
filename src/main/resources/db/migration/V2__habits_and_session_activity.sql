ALTER TABLE account_session ADD COLUMN last_seen_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE habit (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    name varchar(100) NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 100),
    schedule_type varchar(20) NOT NULL CHECK (schedule_type IN ('DAILY', 'SELECTED_DAYS', 'WEEKLY_GOAL')),
    scheduled_days integer[] NOT NULL DEFAULT '{}',
    weekly_target integer NOT NULL DEFAULT 0,
    start_date date NOT NULL,
    archived_on date,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT schedule_shape CHECK (
        (schedule_type='DAILY' AND cardinality(scheduled_days)=0 AND weekly_target=0) OR
        (schedule_type='SELECTED_DAYS' AND cardinality(scheduled_days)>0 AND scheduled_days <@ ARRAY[1,2,3,4,5,6,7] AND weekly_target=0) OR
        (schedule_type='WEEKLY_GOAL' AND cardinality(scheduled_days)=0 AND weekly_target BETWEEN 1 AND 7)
    ),
    CONSTRAINT archive_after_start CHECK (archived_on IS NULL OR archived_on >= start_date),
    UNIQUE (account_id, id)
);
CREATE INDEX habit_account_idx ON habit(account_id);

CREATE TABLE habit_completion (
    account_id uuid NOT NULL,
    habit_id uuid NOT NULL,
    completion_date date NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(habit_id, completion_date),
    FOREIGN KEY(account_id, habit_id) REFERENCES habit(account_id, id) ON DELETE CASCADE
);
CREATE INDEX completion_account_date_idx ON habit_completion(account_id, completion_date);

-- Global, short-lived abuse buckets: no names, emails or IP addresses stored.
CREATE TABLE auth_rate_bucket (
    kind varchar(16) NOT NULL,
    window_start timestamptz NOT NULL,
    attempts integer NOT NULL,
    PRIMARY KEY(kind, window_start)
);
