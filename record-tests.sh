#!/usr/bin/env bash
# Zeichnet die Requests eines Playwright-Tests als WireMock-Stubs auf.
# Voraussetzung: docker compose -f compose.yaml -f compose.recording.yaml up --detach
set -uo pipefail

record_e2e_test() {
  local testFile="$1" testName="$2" issueId="$3"

  curl --silent --request POST http://localhost:8082/__admin/recordings/start \
       --header 'Content-Type: application/json' \
       --data "{
         \"targetBaseUrl\": \"https://overpass-api.de\",
         \"filters\": { \"urlPath\": \"/api/interpreter\", \"method\": \"POST\" },
         \"requestBodyPattern\": { \"matcher\": \"equalTo\" },
         \"repeatsAsScenarios\": false,
         \"transformerParameters\": {
           \"stubName\": \"${testFile}-${testName// /-}\"
         }
       }"

  docker compose exec e2e-tests npx playwright test "tests/${testFile}.spec.ts" -g "${testName}"

  curl --silent --request POST http://localhost:8082/__admin/recordings/stop
}

record_e2e_test "app" "neue Trinkwasserstellen in Bonn" "BF-42"
