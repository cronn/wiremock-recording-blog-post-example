package de.cronn.wiremock.recording.extension;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.StubMappingTransformer;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.EqualToXmlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class OverpassStubMappingTransformer extends StubMappingTransformer {

    // Inhalte von Attributen, die bei jeder Abfrage neu berechnet werden
    private static final Pattern DYNAMIC_VALUES = Pattern.compile("(?<=\\bthan=\")[^\"]*(?=\")");
    // $ muss escaped werden, da replaceAll ${...} sonst als Referenz auf eine Gruppe interpretiert
    private static final String IGNORE = "\\${xmlunit.ignore}";

    @Override
    public String getName() {
        return "overpass-stub-mapping-transformer";
    }

    @Override
    public StubMapping transform(StubMapping stubMapping, FileSource files, Parameters parameters) {
        List<ContentPattern<?>> bodyPatterns = stubMapping.getRequest().getBodyPatterns();
        if (bodyPatterns != null) {
            bodyPatterns.replaceAll(OverpassStubMappingTransformer::equalToPatternToEqualToXml);
        }

        Optional.ofNullable(parameters)
                .map(p -> p.getString("stubName", null))
                .ifPresent(stubMapping::setName);

        return stubMapping;
    }

    private static ContentPattern<?> equalToPatternToEqualToXml(ContentPattern<?> pattern) {
        if (!(pattern instanceof EqualToPattern equalTo)) {
            return pattern;
        }
        String xml = DYNAMIC_VALUES.matcher(equalTo.getExpected()).replaceAll(IGNORE);
        // Erstellt EqualToXmlPattern und aktiviert Platzhalter sowie das Ignorieren der Knotenreihenfolge bei gleichen Namen
        return new EqualToXmlPattern(xml, true, true);
    }
}
