#!/bin/bash
# Wait for Keycloak to be ready
KEYCLOAK_URL="http://keycloak:8080"

echo "Waiting for Keycloak to start at $KEYCLOAK_URL..."
until /opt/keycloak/bin/kcadm.sh config credentials \
  --server $KEYCLOAK_URL \
  --realm master \
  --user admin \
  --password admin123 2>/dev/null; do
  echo "Keycloak not ready yet, retrying in 5 seconds..."
  sleep 5
done

echo "Disabling SSL for master realm..."
/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE

echo "Disabling SSL for brokage realm..."
/opt/keycloak/bin/kcadm.sh update realms/brokage -s sslRequired=NONE 2>/dev/null || echo "Brokage realm not yet created"

echo "SSL disabled successfully!"
