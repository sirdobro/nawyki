#!/bin/sh
set -eu

# Runs only for a new database volume. Credentials never appear in SQL files
# or command arguments. The superuser is reserved for server administration.
APP_DB_PASSWORD="$(cat /run/secrets/db_app_password)"
export APP_DB_PASSWORD
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1 <<'SQL'
\getenv app_password APP_DB_PASSWORD
CREATE ROLE nawyki LOGIN PASSWORD :'app_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
ALTER DATABASE nawyki OWNER TO nawyki;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO nawyki;
SQL
unset APP_DB_PASSWORD
