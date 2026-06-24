# Automatisch deployen vanaf GitHub (CI/CD)

Met het workflow-bestand `.github/workflows/deploy.yml` rolt elke `git push` naar
de `main`-branch automatisch een nieuwe versie uit naar Cloud Run.

De authenticatie gebeurt **zonder opgeslagen sleutels** via *Workload Identity
Federation* (WIF): GitHub wisselt bij elke run een kortlevend token in. Er staat
dus nooit een geheime sleutel in je repo. Dit is eenmalig instellen (~15 min).

## Eenmalige instellen

Vul je eigen waarden in en draai deze blokken in je terminal (gcloud moet
ingelogd zijn, zie de hoofd-README stap 2):

```bash
export PROJECT_ID="JOUW_PROJECT_ID"
export REPO="JOUW_USERNAME/weather-agent"          # owner/repo, exact zoals op GitHub
export SA="github-deployer"
export PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format="value(projectNumber)")
```

```bash
# 1. Service account die de deploys uitvoert
gcloud iam service-accounts create "$SA" \
  --project="$PROJECT_ID" --display-name="GitHub Actions deployer"

# 2. Rechten: Cloud Run beheren, bouwen, image-opslag
for ROLE in roles/run.admin roles/cloudbuild.builds.editor \
            roles/artifactregistry.admin roles/storage.admin; do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="$ROLE"
done

# 3. Mag "namens" de runtime-service-account draaien
gcloud iam service-accounts add-iam-policy-binding \
  "${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --project="$PROJECT_ID" \
  --member="serviceAccount:${SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

# 4. Workload Identity Pool + provider (vertrouwt GitHub's OIDC)
gcloud iam workload-identity-pools create "github-pool" \
  --project="$PROJECT_ID" --location="global" --display-name="GitHub pool"

gcloud iam workload-identity-pools providers create-oidc "github-provider" \
  --project="$PROJECT_ID" --location="global" \
  --workload-identity-pool="github-pool" --display-name="GitHub provider" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='${REPO}'" \
  --issuer-uri="https://token.actions.githubusercontent.com"

# 5. Sta ALLEEN jouw repo toe om als deze service account te handelen
gcloud iam service-accounts add-iam-policy-binding \
  "${SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project="$PROJECT_ID" --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/attribute.repository/${REPO}"

# 6. Print de twee waarden die je in GitHub nodig hebt
echo "WIF_PROVIDER=projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/providers/github-provider"
echo "WIF_SERVICE_ACCOUNT=${SA}@${PROJECT_ID}.iam.gserviceaccount.com"
```

## In GitHub instellen

Ga in je repo naar **Settings → Secrets and variables → Actions → Variables**
(tabblad *Variables*, niet *Secrets* — deze waarden zijn niet geheim) en voeg toe:

| Variabele | Waarde |
|---|---|
| `GCP_PROJECT_ID` | je project-ID |
| `WIF_PROVIDER` | de `WIF_PROVIDER`-regel uit stap 6 |
| `WIF_SERVICE_ACCOUNT` | de `WIF_SERVICE_ACCOUNT`-regel uit stap 6 |

Klaar. Doe een `git push` naar `main` en kijk bij het tabblad **Actions** hoe de
deploy loopt. De live-URL verschijnt onderaan de logs.

> Klein detail: vanaf 18 juni 2026 zet GitHub voor nieuwe repo's "immutable
> subject claims" standaard aan. De conditie hierboven kijkt naar
> `attribute.repository` en blijft daardoor gewoon werken.

## Liever zonder WIF?

Er bestaat een simpelere maar minder veilige variant met een service-account
JSON-sleutel als GitHub-secret. Dat wordt door Google afgeraden (langlevende
sleutel die kan lekken), dus de keyless opzet hierboven heeft de voorkeur.
