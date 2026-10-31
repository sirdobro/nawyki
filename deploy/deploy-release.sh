#!/bin/bash
set -euo pipefail
[[ $# == 1 && $1 =~ ^[0-9a-f]{40}$ ]] || exit 2
exec 9>/run/lock/nawyki-deploy.lock
flock -w 120 9
cd /opt/nawyki
sha=$1
previous=''
if [[ -f release.env ]]; then previous=$(cat release.env); fi
# Called only by the root-owned pull service; stdin is a checksum-verified image.
timeout 180 docker load
[[ $(docker image inspect "nawyki-app:$sha" --format '{{.Config.User}}') == '10001:10001' ]]
printf 'APP_IMAGE=nawyki-app:%s\nAPP_RELEASE=%s\n' "$sha" "$sha" > release.env
chmod 640 release.env
compose=(docker compose --env-file .env --env-file release.env -f compose.yaml -f compose.app.yaml)
if ! "${compose[@]}" up -d --wait --wait-timeout 150 app < /dev/null; then
    if [[ -n "$previous" ]]; then
        printf '%s\n' "$previous" > release.env
        "${compose[@]}" up -d --wait --wait-timeout 150 app < /dev/null
    else
        "${compose[@]}" stop app < /dev/null
        rm -f release.env
    fi
    echo 'Application health check failed; reverted where a previous release existed.' >&2
    exit 1
fi
cp -p Caddyfile Caddyfile.previous-release
cat Caddyfile.app > Caddyfile
if ! "${compose[@]}" exec -T proxy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile < /dev/null; then
    cat Caddyfile.previous-release > Caddyfile
    "${compose[@]}" exec -T proxy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile < /dev/null
    if [[ -n "$previous" ]]; then
        printf '%s\n' "$previous" > release.env
        "${compose[@]}" up -d --wait --wait-timeout 150 app < /dev/null
    else
        "${compose[@]}" stop app < /dev/null
        rm -f release.env
    fi
    echo 'Proxy reload failed; restored previous proxy configuration.' >&2
    exit 1
fi
echo "Deployed release $sha"
# No volume deletion or automatic DB reset.
