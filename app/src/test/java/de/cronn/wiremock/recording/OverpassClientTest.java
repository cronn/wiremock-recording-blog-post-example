package de.cronn.wiremock.recording;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OverpassClientTest {

    @Test
    void queryContainsDateOneYearAgo() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T13:37:42Z"), ZoneOffset.UTC);
        OverpassClient client = new OverpassClient(RestClient.create(), clock);

        assertThat(client.query()).contains("<newer than=\"2025-10-16T00:00:00Z\"/>");
    }
}
