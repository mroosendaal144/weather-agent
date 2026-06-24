package com.example.weatheragent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ververst het weer automatisch elke dag om 06:00 (Nederlandse tijd).
 *
 * Let op: dit werkt alleen als de applicatie op dat moment draait. Op Cloud Run
 * met "scale-to-zero" staat de applicatie stil als er geen verkeer is, waardoor
 * deze taak niet altijd afgaat. Gebruik daarom op Cloud Run ook Cloud Scheduler
 * (zie README) om dagelijks /api/refresh aan te roepen. De per-dag-cache in
 * WeatherService zorgt er sowieso voor dat de eerste bezoeker van een nieuwe dag
 * verse data te zien krijgt.
 */
@Component
public class ScheduledRefresh {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRefresh.class);

    private final WeatherService weatherService;

    public ScheduledRefresh(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Europe/Amsterdam")
    public void daily() {
        try {
            weatherService.refresh();
        } catch (Exception e) {
            log.warn("Dagelijkse weer-refresh mislukt: {}", e.getMessage());
        }
    }
}
