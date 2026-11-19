#!/bin/sh
set -eu

project_dir=$(cd "$(dirname "$0")/.." && pwd)

work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT
if [ "$#" -eq 2 ]; then
    server_url=$1
    cp "$2" "$work_dir/server.crt"
elif [ "$#" -eq 0 ]; then
    if ! docker compose version >/dev/null 2>&1; then
        printf '%s\n' 'Install and start Docker with Compose.' >&2
        exit 1
    fi
    cd "$project_dir"
    server_url="https://${SPOTTER_LAN_IP:-127.0.0.1}:8443/chat"
    docker compose up -d --build
    for attempt in 1 2 3 4 5 6 7 8 9 10; do
        if docker compose exec -T server cat /data/server.crt > "$work_dir/server.crt" 2>/dev/null &&
            curl --noproxy '*' --cacert "$work_dir/server.crt" --max-time 2 --silent \
                --output /dev/null "$server_url"; then
            break
        fi
        if [ "$attempt" -eq 10 ]; then
            printf '%s\n' 'The Spotter server did not start. Run docker compose logs server.' >&2
            exit 1
        fi
        sleep 1
    done
else
    printf '%s\n' 'Usage: sh scripts/spotter-docker.sh [HTTPS server URL public certificate]' >&2
    exit 1
fi

cd "$project_dir"
SPOTTER_TRUST_PASSWORD=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
export SPOTTER_TRUST_PASSWORD
keytool -importcert -noprompt -alias spotter -file "$work_dir/server.crt" \
    -storetype PKCS12 -keystore "$work_dir/trust.p12" -storepass:env SPOTTER_TRUST_PASSWORD
mkdir "$work_dir/classes"
find src/spotter -maxdepth 1 -name '*.java' ! -name '*Test.java' -exec \
    javac -d "$work_dir/classes" {} +
java -Djavax.net.ssl.trustStore="$work_dir/trust.p12" -Djavax.net.ssl.trustStoreType=PKCS12 \
    -Djavax.net.ssl.trustStorePassword="$SPOTTER_TRUST_PASSWORD" \
    -cp "$work_dir/classes" spotter.SpotterLanView "$server_url"
