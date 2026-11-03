CREATE TABLE workout (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    workout_date date NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX workout_account_date_idx ON workout(account_id, workout_date DESC, created_at DESC);

CREATE TABLE workout_exercise (
    id uuid PRIMARY KEY,
    workout_id uuid NOT NULL REFERENCES workout(id) ON DELETE CASCADE,
    position integer NOT NULL CHECK (position BETWEEN 0 AND 29),
    name varchar(100) NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 100),
    UNIQUE(workout_id, position)
);

CREATE TABLE workout_set (
    exercise_id uuid NOT NULL REFERENCES workout_exercise(id) ON DELETE CASCADE,
    position integer NOT NULL CHECK (position BETWEEN 0 AND 29),
    repetitions integer NOT NULL CHECK (repetitions BETWEEN 1 AND 10000),
    weight_kg numeric(7,3) CHECK (weight_kg >= 0 AND weight_kg <= 9999.999),
    PRIMARY KEY(exercise_id, position)
);
