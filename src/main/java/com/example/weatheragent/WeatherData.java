package com.example.weatheragent;

/**
 * De weergegevens die we teruggeven aan de webinterface (als JSON).
 * Een 'record' is een compacte, onveranderlijke datadrager in Java.
 */
public record WeatherData(
        String location,            // plaatsnaam
        String observedAt,          // tijdstip van de meting (van Open-Meteo)
        String fetchedAt,           // wanneer onze service het ophaalde
        double temperature,         // huidige temperatuur (°C)
        double apparentTemperature, // gevoelstemperatuur (°C)
        int humidity,               // luchtvochtigheid (%)
        double windSpeed,           // windsnelheid (km/u)
        double tempMax,             // maximum vandaag (°C)
        double tempMin,             // minimum vandaag (°C)
        double precipitation,       // neerslag vandaag (mm)
        int weatherCode,            // WMO-weercode
        String description,         // Nederlandse omschrijving
        String emoji                // passend icoon
) {
}
