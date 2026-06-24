# Weeragent · Culemborg 🌤️

Je eerste "hello-world" agent: een kleine Java-applicatie (Spring Boot) met een
webinterface die elke dag het weer in **Culemborg** ophaalt en toont. Bedoeld om
de techniek end-to-end werkend te krijgen op **Google Cloud**, als basis om later
echte AI-functionaliteit aan toe te voegen.

> Eerlijk kader: deze eerste versie is technisch een **webapp + dagelijkse taak**,
> nog geen LLM-gestuurde "agent". De structuur (web-UI, API, planner, deployment)
> is precies wat je nodig hebt om er straks een echte AI-agent van te maken — zie
> *Volgende stappen* onderaan.

---

## Wat zit erin?

| Onderdeel | Bestand | Functie |
|---|---|---|
| Applicatie-start | `WeatherAgentApplication.java` | Start de app, zet planning aan |
| Weer ophalen + cache | `WeatherService.java` | Roept Open-Meteo aan, bewaart per dag |
| API | `WeatherController.java` | `/api/weather`, `/api/refresh`, `/api/health` |
| Dagelijkse taak | `ScheduledRefresh.java` | Ververst elke dag om 06:00 |
| Webinterface | `static/index.html` | Toont het weer in de browser |
| Container | `Dockerfile` | Bouwt en draait de app op Cloud Run |

De weerdata komt van **[Open-Meteo](https://open-meteo.com/)**: gratis en
**zonder API-key** voor niet-commercieel gebruik.

---

## 1. Lokaal uitproberen (optioneel maar aanrader)

Je hebt **Java 21** en **Maven** nodig (of gebruik Docker, zie stap 2).

```bash
cd weather-agent
mvn spring-boot:run
```

Open daarna http://localhost:8080 in je browser. Je ziet de weerkaart.
Test eventueel de API direct: http://localhost:8080/api/weather

---

## 2. Voorbereiden op Google Cloud (eenmalig)

1. **Google Cloud account + project.** Maak via https://console.cloud.google.com
   een project aan (bijv. `weeragent`) en zet **facturering (billing)** aan. De
   gratis tier dekt dit project ruimschoots — bij geen verkeer betaal je ~niets.
2. **Installeer de `gcloud` CLI:** https://cloud.google.com/sdk/docs/install
3. **Inloggen en project kiezen** (vervang `JOUW_PROJECT_ID`):

   ```bash
   gcloud auth login
   gcloud config set project JOUW_PROJECT_ID
   ```

4. **Benodigde API's aanzetten:**

   ```bash
   gcloud services enable \
     run.googleapis.com \
     cloudbuild.googleapis.com \
     artifactregistry.googleapis.com \
     cloudscheduler.googleapis.com
   ```

> Je hoeft **geen Java of Docker lokaal** te hebben voor de deploy: Cloud Build
> bouwt de container in de cloud op basis van de `Dockerfile`.

---

## 3. Deployen naar Cloud Run

Vanuit de map `weather-agent`:

```bash
gcloud run deploy weather-agent \
  --source . \
  --region europe-west4 \
  --allow-unauthenticated
```

- `--source .` → Cloud Build bouwt de container uit de `Dockerfile`.
- `europe-west4` is Nederland (Eemshaven).
- `--allow-unauthenticated` maakt de webpagina publiek bereikbaar.

Aan het einde krijg je een URL te zien, bijv.
`https://weather-agent-xxxx-ez.a.run.app`. Open die in je browser — klaar! 🎉

---

## 4. Echt elke dag verversen (Cloud Scheduler)

Cloud Run zet de app op slaap als er geen verkeer is, dus de ingebouwde planner
draait niet altijd. Laat **Cloud Scheduler** daarom dagelijks `/api/refresh`
aanroepen. Vervang `SERVICE_URL` door de URL uit stap 3:

```bash
gcloud scheduler jobs create http weather-daily \
  --location europe-west4 \
  --schedule "0 6 * * *" \
  --time-zone "Europe/Amsterdam" \
  --uri "SERVICE_URL/api/refresh" \
  --http-method POST
```

(De per-dag-cache zorgt er sowieso voor dat de eerste bezoeker van een nieuwe dag
verse data ziet; Scheduler is voor een gegarandeerde dagelijkse ophaalactie.)

---

## 5. Bijwerken & opruimen

- **Nieuwe versie uitrollen:** draai het `gcloud run deploy`-commando opnieuw.
- **Logs bekijken:** `gcloud run services logs read weather-agent --region europe-west4`
- **Alles verwijderen** (om kosten te stoppen):

  ```bash
  gcloud scheduler jobs delete weather-daily --location europe-west4
  gcloud run services delete weather-agent --region europe-west4
  ```

---

## Volgende stappen: van weerapp naar échte AI-agent

1. **Een LLM toevoegen.** Roep bijv. de Claude API aan om de ruwe weerdata om te
   zetten in advies ("Neem een jas mee, kans op buien vanmiddag").
2. **Tools / acties geven.** Laat de agent meer API's bevragen (agenda, verkeer)
   en redeneren over wat hij ophaalt.
3. **Geheugen.** Bewaar historie in een database (bijv. Firestore) zodat de agent
   trends kan benoemen.
4. **Secrets netjes regelen.** Zet API-sleutels in Google Secret Manager in plaats
   van in de code.

---

## Probleemoplossing

- **Build faalt op Maven Central:** controleer je internetverbinding; Cloud Build
  haalt dependencies online op.
- **`403` of pagina niet bereikbaar:** controleer of je `--allow-unauthenticated`
  hebt meegegeven bij de deploy.
- **Geen weerdata:** test `SERVICE_URL/api/health` (moet `OK` geven) en bekijk de
  logs (zie stap 5).
