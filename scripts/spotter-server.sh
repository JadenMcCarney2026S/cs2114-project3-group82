#!/bin/sh
set -eu
umask 077

data_dir=${SPOTTER_DATA_DIR:-/data}
host_ip=${SPOTTER_LAN_IP:-127.0.0.1}
case "$host_ip" in
    *[!0-9.]*|0.0.0.0) printf '%s\n' 'Set SPOTTER_LAN_IP to the host IPv4 address.' >&2; exit 1 ;;
esac
certificate="$data_dir/server.crt"
keystore="$data_dir/server-$host_ip.p12"
password_file="$data_dir/keystore-password"

if [ ! -s "$password_file" ]; then
    if [ -e "$keystore" ]; then
        printf '%s\n' 'The server keystore password is missing.' >&2
        exit 1
    fi
    od -An -N24 -tx1 /dev/urandom | tr -d ' \n' > "$password_file.tmp"
    mv "$password_file.tmp" "$password_file"
fi
SPOTTER_KEYSTORE_PASSWORD=$(cat "$password_file")
export SPOTTER_KEYSTORE_PASSWORD

if [ ! -s "$keystore" ]; then
    rm -f "$keystore.tmp"
    keytool -genkeypair -alias spotter -keyalg RSA -keysize 2048 \
        -dname 'CN=Spotter' -ext "SAN=ip:$host_ip" -validity 3650 \
        -storetype PKCS12 -keystore "$keystore.tmp" \
        -storepass:env SPOTTER_KEYSTORE_PASSWORD
    mv "$keystore.tmp" "$keystore"
fi
keytool -exportcert -rfc -alias spotter -keystore "$keystore" \
    -file "$certificate.tmp" -storepass:env SPOTTER_KEYSTORE_PASSWORD
mv "$certificate.tmp" "$certificate"

exec java -Djava.awt.headless=true -cp "${SPOTTER_CLASSES:-/app}" \
    spotter.SpotterLanServer "${SPOTTER_BIND:-0.0.0.0}" "${SPOTTER_PORT:-8080}" \
    "$keystore" "$data_dir/accounts.dat"
