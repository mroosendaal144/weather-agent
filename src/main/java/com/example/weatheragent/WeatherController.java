package com.example.weatheragent;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stelt de weergegevens beschikbaar als JSON.
 * De webinterface (static/index.html) roept GET /api/weather aan.
 */
@RestController
@RequestMapping("/api")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /** Huidige (per dag gecachete) weergegevens. */
    @GetMapping("/weather")
    public WeatherData weather() {
        return weatherService.getWeather();
    }

    /** Forceert een verse ophaalactie. Wordt o.a. door Cloud Scheduler aangeroepen. */
    @PostMapping("/refresh")
    public WeatherData refresh() {
        return weatherService.refresh();
    }

    /** Eenvoudige health-check. */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
