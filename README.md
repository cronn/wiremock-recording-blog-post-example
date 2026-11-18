# Example Code for the Blog Post About WireMock Recording and Playback

The project combines the listings used in this blog post into one project. The code is not suitable for production and are used for illustrative purposes only.

## Recording

The recording calls the real Overpass API. Under heavy load, the Overpass API responds slowly or returns `502`/`504` errors. WireMock records these error responses, too; they must then be deleted, and the recording repeated. There is already a recording saved in `wiremock/mappings`. Remove it before starting a new recording.

> [!NOTE]
> The Playwright and WireMock containers run with `UID` and `GID` set to `1000` instead of `root`. If your `UID` or `GID` differs from `1000`, set them manually.

1. Build the WireMock extension and copy it to the `wiremock/extensions` directory:
    ```shell 
    ./gradlew build :wiremock-extension:copyToWireMock
    ```
2. Start the test environment without WireMock and create or update snapshots. Alternatively, run the test using the Playwright UI at http://localhost:8095 by selecting “Update snapshots.”
    ```shell 
    docker compose up --detach --build --force-recreate 
    docker compose exec e2e-tests npx playwright test --update-snapshots
    ```
3. Start the test environment with WireMock as a proxy and record the test:
    ```shell
    docker compose -f compose.recording.yaml up --detach 
    ./record-tests.sh
    # Check that the recorded stub has response.status = 200 
    docker compose -f compose.recording.yaml down
    ```

The stub mapping is saved in `wiremock/mappings`. WireMock stores responses larger than 10 KB in `wiremock/files`.

## Playback

Playback does not require a connection to the Overpass API. WireMock responds to the application’s requests using the checked-in stub mappings from `wiremock/mappings`:

```shell
docker compose -f compose.mock.yaml up --detach
docker compose exec --no-tty e2e-tests npx playwright test
```
