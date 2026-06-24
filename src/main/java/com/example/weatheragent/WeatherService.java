package com.example.weatheragent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Haalt het actuele weer voor Culemborg op bij de gratis Open-Meteo API
 * (geen API-key nodig) en bewaart het resultaat in het geheugen.
 *
 * De cache wordt per dag ververst: de eerste aanvraag van een nieuwe dag
 * haalt verse data op. Daarnaast kan de data geforceerd worden ververst via
 * refresh() (gebruikt door de dagelijkse planner en het /api/refresh endpoint).
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    // Coordinaten van Culemborg, Nederland
    private static final double LAT = 51.954;
    private static final double LON = 5.227;
    private static final String LOCATION_NAME = "Culemborg";
    private static final ZoneId ZONE = ZoneId.of("Europe/Amsterdam");

    private static final String URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=" + LAT
            + "&longitude=" + LON
            + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum"
            + "&timezone=Europe%2FAmsterdam"
            + "&forecast_days=1";

    private static final DateTimeFormatter NL_TIME =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final RestClient restClient = RestClient.create();

    // Eenvoudige in-memory cache
    private volatile WeatherData cached;
    private volatile LocalDate cachedDate;

    /** Geeft het weer terug; haalt alleen verse data op als we vandaag nog niets hebben. */
    public synchronized WeatherData getWeather() {
        LocalDate today = LocalDate.now(ZONE);
        if (cached == null || !today.equals(cachedDate)) {
            refresh();
        }
        return cached;
    }

    /** Forceert een nieuwe ophaalactie bij Open-Meteo. */
    public synchronized WeatherData refresh() {
        log.info("Weer ophalen voor {}...", LOCATION_NAME);

        OpenMeteoResponse resp = restClient.get()
                .uri(URL)
                .retrieve()
                .body(OpenMeteoResponse.class);

        if (resp == null || resp.current() == null) {
            throw new IllegalStateException("Onverwachte of lege respons van Open-Meteo");
        }

        OpenMeteoResponse.Current c = resp.current();
        OpenMeteoResponse.Daily d = resp.daily();
        int code = c.weatherCode();

        WeatherData data = new WeatherData(
                LOCATION_NAME,
                c.time(),
                ZonedDateTime.now(ZONE).format(NL_TIME),
                c.temperature(),
                c.apparentTemperature(),
                c.humidity(),
                c.windSpeed(),
                first(d == null ? null : d.tempMax()),
                first(d == null ? null : d.tempMin()),
                first(d == null ? null : d.precipitation()),
                code,
                describe(code),
                emoji(code)
        );

        this.cached = data;
        this.cachedDate = LocalDate.now(ZONE);
        log.info("Weer opgehaald: {} graden C, {}", data.temperature(), data.description());
        return data;
    }

    /** Eerste waarde uit een dag-lijst, of 0 als die ontbreekt. */
    private static double first(List<Double> values) {
        return (values == null || values.isEmpty()) ? 0.0 : values.get(0);
    }

    /** Zet een WMO-weercode om naar een Nederlandse omschrijving. */
    private static String describe(int code) {
        return switch (code) {
            case 0 -> "Helder";
            case 1 -> "Overwegend helder";
            case 2 -> "Half bewolkt";
            case 3 -> "Bewolkt";
            case 45, 48 -> "Mist";
            case 51, 53, 55 -> "Motregen";
            case 56, 57 -> "IJzel (motregen)";
            case 61, 63, 65 -> "Regen";
            case 66, 67 -> "IJzel (regen)";
            case 71, 73, 75 -> "Sneeuw";
            case 77 -> "Sneeuwkorrels";
            case 80, 81, 82 -> "Regenbuien";
            case 85, 86 -> "Sneeuwbuien";
            case 95 -> "Onweer";
            case 96, 99 -> "Onweer met hagel";
            default -> "Onbekend";
        };
    }

    /** Passend icoon bij de weercode. */
    private static String emoji(int code) {
        return switch (code) {
            case 0, 1 -> "\u2600\uFE0F";                       // zon
            case 2 -> "\u26C5";                                 // half bewolkt
            case 3 -> "\u2601\uFE0F";                          // bewolkt
            case 45, 48 -> "\uD83C\uDF2B\uFE0F";              // mist
            case 51, 53, 55, 56, 57 -> "\uD83C\uDF26\uFE0F";  // motregen
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "\uD83C\uDF27\uFE0F"; // regen
            case 71, 73, 75, 77, 85, 86 -> "\u2744\uFE0F";    // sneeuw
            case 95, 96, 99 -> "\u26C8\uFE0F";                // onweer
            default -> "\uD83C\uDF21\uFE0F";                   // thermometer
        };
    }
}
