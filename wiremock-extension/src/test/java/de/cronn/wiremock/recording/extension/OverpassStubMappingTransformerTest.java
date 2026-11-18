package de.cronn.wiremock.recording.extension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.EqualToXmlPattern;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

class OverpassStubMappingTransformerTest {

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
    private static final String RESPONSE = "{\"elements\":[{\"type\":\"node\",\"id\":42}]}";

    // Stellvertreter für die Overpass-API
    @RegisterExtension
    static WireMockExtension upstream = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    // Zeichnet mit der Extension gegen den Upstream auf
    @RegisterExtension
    static WireMockExtension recorder = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().extensions(new OverpassExtensionFactory()))
        .build();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        // Mit Content-Type legt WireMock den kurzen Body im Stub ab, statt ihn als Datei zu extrahieren
        upstream.stubFor(post("/api/interpreter").willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(RESPONSE)));
    }

    @Test
    void replacesBodyPatternWithEqualToXmlIgnoringDynamicValues() throws Exception {
        StubMapping recorded = record(Parameters.empty());

        EqualToXmlPattern bodyPattern = assertInstanceOf(EqualToXmlPattern.class,
            recorded.getRequest().getBodyPatterns().get(0));
        assertTrue(bodyPattern.getExpected().contains("<newer than=\"${xmlunit.ignore}\"/>"));
        assertTrue(bodyPattern.isEnablePlaceholders());
        assertTrue(bodyPattern.isIgnoreOrderOfSameNode());
    }

    @Test
    void setsNameAndIssueFromTransformerParameters() throws Exception {
        StubMapping recorded = record(Parameters.from(Map.of("stubName", "app-test", "issueId", "BF-42")));

        assertEquals("app-test", recorded.getName());
    }

    @Test
    void keepsNameWithoutTransformerParameters() throws Exception {
        StubMapping recorded = record(Parameters.empty());

        assertEquals("api_interpreter", recorded.getName());
    }

    @Test
    void recordedStubMatchesRequestWithOtherDate() throws Exception {
        StubMapping recorded = record(Parameters.empty());
        recorder.resetAll();
        recorder.addStubMapping(recorded);

        // Anders formatiert und mit anderem Datum als bei der Aufzeichnung
        String otherQuery = QUERY.formatted("2030-01-01T00:00:00Z").replace("\n  ", "\n\t");
        HttpResponse<String> response = sendQuery(otherQuery);

        assertEquals(200, response.statusCode());
        assertEquals(RESPONSE, response.body());
    }

    @Test
    void recordedStubDoesNotMatchOtherQuery() throws Exception {
        StubMapping recorded = record(Parameters.empty());
        recorder.resetAll();
        recorder.addStubMapping(recorded);

        String otherQuery = QUERY.formatted("2025-10-16T00:00:00Z").replace("drinking_water", "fountain");
        HttpResponse<String> response = sendQuery(otherQuery);

        assertEquals(404, response.statusCode());
    }

    private StubMapping record(Parameters transformerParameters) throws Exception {
        recorder.startRecording(new RecordSpecBuilder()
            .forTarget(upstream.baseUrl())
            .matchRequestBodyWithEqualTo()
            .transformerParameters(transformerParameters)
            .makeStubsPersistent(false));

        HttpResponse<String> response = sendQuery(QUERY.formatted("2025-10-16T00:00:00Z"));
        assertEquals(200, response.statusCode());

        List<StubMapping> stubMappings = recorder.stopRecording().getStubMappings();
        assertEquals(1, stubMappings.size());
        return stubMappings.get(0);
    }

    private HttpResponse<String> sendQuery(String query) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(recorder.baseUrl() + "/api/interpreter"))
            .header("Content-Type", "text/xml")
            .POST(HttpRequest.BodyPublishers.ofString(query))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
