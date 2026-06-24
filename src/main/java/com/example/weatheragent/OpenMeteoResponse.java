package com.example.weatheragent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mapt de JSON-respons van Open-Meteo naar Java-objecten.
 *
 * De JSON gebruikt namen als "temperature_2m"; met @JsonProperty koppelen we die
 * aan nette Java-namen. @JsonIgnoreProperties zorgt dat extra velden die we niet
 * gebruiken (zoals "interval" of "*_units") gewoon genegeerd worden.
 *
 * Let op: deze annotaties komen uit com.fasterxml.jackson.annotation — dat pakket
 * blijft in Jackson 3 ongewijzigd (alleen de databind-klassen verhuisden naar
 * tools.jackson). Daarom is hier geen import uit com.fasterxml.jackson.databind.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoResponse(Current current, Daily daily) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            String time,
            @JsonProperty("temperature_2m") double temperature,
            @JsonProperty("relative_humidity_2m") int humidity,
            @JsonProperty("apparent_temperature") double apparentTemperature,
            @JsonProperty("weather_code") int weatherCode,
            @JsonProperty("wind_speed_10m") double windSpeed
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Daily(
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("temperature_2m_max") List<Double> tempMax,
            @JsonProperty("temperature_2m_min") List<Double> tempMin,
            @JsonProperty("precipitation_sum") List<Double> precipitation
    ) {
    }
}
