package de.cronn.wiremock.recording;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DrinkingWaterController {

    private final OverpassClient overpassClient;

    public DrinkingWaterController(OverpassClient overpassClient) {
        this.overpassClient = overpassClient;
    }

    @GetMapping("/api/drinking-water")
    public List<DrinkingWater> findNewDrinkingWater() {
        return overpassClient.findNewDrinkingWater().stream()
            .map(element -> new DrinkingWater(
                element.id(),
                element.lat(),
                element.lon(),
                element.tags() == null ? null : element.tags().get("name"),
                element.timestamp(),
                element.version()))
            .sorted(Comparator.comparing(DrinkingWater::id))
            .toList();
    }

    public record DrinkingWater(long id, double lat, double lon, String name, Instant timestamp, int version) {
    }
}
