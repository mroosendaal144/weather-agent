package com.example.weatheragent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Startpunt van de applicatie.
 * @EnableScheduling zet de dagelijkse weer-refresh aan (zie ScheduledRefresh).
 */
@SpringBootApplication
@EnableScheduling
public class WeatherAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherAgentApplication.class, args);
    }
}
