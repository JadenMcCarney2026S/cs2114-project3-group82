#!/bin/sh
set -eu

if [ "$#" -gt 1 ]; then
    printf '%s\n' 'Usage: sh scripts/spotter-android.sh [public certificate for local port 8443]' >&2
    exit 1
fi

project_dir=$(cd "$(dirname "$0")/.." && pwd)
certificate=${1:-}
if [ -n "$certificate" ]; then
    certificate="$(cd "$(dirname "$certificate")" && pwd)/$(basename "$certificate")"
fi

sdk_dir=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
if [ -z "$sdk_dir" ] && [ -d "$HOME/Library/Android/sdk" ]; then
    sdk_dir="$HOME/Library/Android/sdk"
fi
if [ -n "$sdk_dir" ]; then
    export ANDROID_HOME="$sdk_dir"
fi
adb_bin=$(command -v adb || true)
if [ -z "$adb_bin" ] && [ -n "$sdk_dir" ]; then
    adb_bin="$sdk_dir/platform-tools/adb"
fi
if [ -z "$adb_bin" ] || [ ! -x "$adb_bin" ]; then
    printf '%s\n' 'Install Android SDK Platform-Tools and start an Android emulator.' >&2
    exit 1
fi
if ! "$adb_bin" get-state >/dev/null 2>&1; then
    printf '%s\n' 'Start one Android emulator before running this command.' >&2
    exit 1
fi

cd "$project_dir"
work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT
server_url=https://127.0.0.1:8443/chat

if [ -n "$certificate" ]; then
    cp "$certificate" "$work_dir/server.crt"
else
    if ! docker compose version >/dev/null 2>&1; then
        printf '%s\n' 'Start Docker with Compose or pass the public certificate of a running server.' >&2
        exit 1
    fi
    export SPOTTER_LAN_IP=127.0.0.1
    docker compose up -d --build
fi

for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if [ -z "$certificate" ]; then
        docker compose exec -T server cat /data/server.crt > "$work_dir/server.crt" 2>/dev/null || true
    fi
    if curl --noproxy '*' --cacert "$work_dir/server.crt" --max-time 2 --silent \
        --output /dev/null "$server_url"; then
        break
    fi
    if [ "$attempt" -eq 10 ]; then
        printf '%s\n' 'Could not verify the local Spotter HTTPS server on port 8443.' >&2
        exit 1
    fi
    sleep 1
done

mkdir -p android/app/src/main/assets
cp "$work_dir/server.crt" android/app/src/main/assets/spotter-server.crt
android/gradlew -p android :app:assembleDebug
"$adb_bin" reverse tcp:8443 tcp:8443
"$adb_bin" install -r android/app/build/outputs/apk/debug/app-debug.apk
"$adb_bin" shell am start -n spotter.mobile/.MainActivity
