package de.cronn.wiremock.recording;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OverpassClient {

    private static final String QUERY = """
        <osm-script output="json" timeout="25">
          <query type="node">
            <has-kv k="amenity" v="drinking_water"/>
            <bbox-query s="50.725" w="7.085" n="50.740" e="7.110"/>
            <newer than="%s"/>
          </query>
          <print mode="meta"/>
        </osm-script>
        """;

    private static final String USER_AGENT = "app (https://github.com/cronn/wiremock-recording-blog-post-example)";

    private final RestClient restClient;
    private final Clock clock;

    @Autowired
    public OverpassClient(RestClient.Builder restClientBuilder, @Value("${overpass.url}") String overpassUrl) {
        this(restClientBuilder
                .baseUrl(overpassUrl)
                // Die Overpass-API lehnt Requests ohne aussagekräftigen User-Agent ab
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build(),
            Clock.systemUTC());
    }

    OverpassClient(RestClient restClient, Clock clock) {
        this.restClient = restClient;
        this.clock = clock;
    }

    public List<OverpassElement> findNewDrinkingWater() {
        OverpassResponse response = restClient.post()
            .contentType(MediaType.TEXT_XML)
            .body(query())
            .retrieve()
            .body(OverpassResponse.class);
        return response == null ? List.of() : response.elements();
    }

    String query() {
        Instant oneYearAgo = ZonedDateTime.now(clock)
            .minusMonths(12)
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant();
        return QUERY.formatted(oneYearAgo);
    }

    record OverpassResponse(List<OverpassElement> elements) {
    }

    public record OverpassElement(long id, double lat, double lon, Instant timestamp, int version,
                                  Map<String, String> tags) {
    }
}
