# Flatina Fresh

A home-cooked HelloFresh: keep your recipe cards in one place, plan meals on a weekly calendar, and get a
combined shopping list for the week.

- **Recipes**: scan both sides of a HelloFresh card (Claude reads it into title, times, 2- and 4-person
  ingredient amounts, pantry items and steps, then you review it), add one by hand, or import a bundle.
- **Meal plan**: pick recipes for each day of the week, at 2 or 4 servings.
- **Shopping list**: ingredients for the week, added up across recipes (tbsp/tsp/cups are combined),
  grouped by store section, with pantry staples listed separately. Check items off as you shop.
- **My products**: save the brand, package size and store item number (or link) you buy for an ingredient, once.
  The shopping list then shows that product and how many packages cover the week (6 tbsp soy sauce is one
  10 fl oz bottle), and items you mark as "always have" start out ticked. **Copy order list** copies everything
  still unticked, grouped by aisle, to paste beside a store's site.
- **Admin**: add other users, reset passwords, and import recipe bundles.

The stack mirrors RPOC: Spring Boot 3.5 (Java 21) + PostgreSQL + Flyway for the API, Vue 3 for the client,
deployed to Cloud Run by GitHub Actions.

```
flatina-fresh-api/      Spring Boot API (port 8080)
flatina-fresh-client/   Vue 3 + Vite client, served by nginx in its container
gcp-deployment/         setup.sh: provisions the Google Cloud project and GitHub deploy access
.github/workflows/      tests.yml (PRs and main), deploy.yml (Cloud Run)
```

## Run it locally

Needs Java 21, Maven, Node 22, and PostgreSQL (or Docker).

```bash
docker compose up -d                         # PostgreSQL on localhost:5432 (db/user/password: flatina)

cd flatina-fresh-api
ADMIN_USERNAME=tom ADMIN_PASSWORD=change-me mvn spring-boot:run

cd ../flatina-fresh-client
npm install
npm run dev                                  # http://localhost:5173
```

Sign in with the `ADMIN_USERNAME` / `ADMIN_PASSWORD` you started the API with. That account is created on
first start; after that, change its password in the app.

To try card scanning locally, also set `ANTHROPIC_API_KEY` when starting the API.

Tests: `mvn verify` in `flatina-fresh-api` (uses an in-memory H2 database), and `npm run lint && npm run build`
in `flatina-fresh-client`.

## API configuration

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection | local `flatina` database |
| `JWT_SECRET` | Signs login tokens | dev-only value |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | First admin account, created if missing | `admin`, none |
| `CORS_ALLOWED_ORIGINS` | Comma-separated client origins | `http://localhost:5173` |
| `ANTHROPIC_API_KEY` | Enables card scanning | none (scanning returns an error) |
| `SCAN_MODEL` | Claude model for scanning | `claude-opus-5-5` |

The client reads its API URL at runtime from `config.js`, which the container writes from `API_URL`.

## Deploying to Google Cloud

Flatina Fresh gets its own Google Cloud project so its billing, access and data stay separate from RPOC.

1. Run `gcp-deployment/setup.sh` (needs `gcloud` signed in as an owner, and a billing account). It creates the
   project, Artifact Registry, a Cloud SQL PostgreSQL 16 instance (`db-f1-micro`), a runtime service account,
   and a `github-deployer` service account that GitHub Actions can use through Workload Identity Federation
   (only from this repository). If `gh` is signed in, it also sets the GitHub environment variables and
   secrets; otherwise it prints them. Answers are saved to `gcp-deployment/deploy-config.env` (git-ignored).
2. Optional: add an `ANTHROPIC_API_KEY` secret to the `development` environment to turn on card scanning.
3. Push to `main` (or run **Build and Deploy** by hand). The workflow builds both images, deploys the
   `flatina-api` and `flatina-client` Cloud Run services, and prints the app URL in the run summary.
   Database migrations run when the API starts.

Publishing a GitHub release deploys to the `production` environment instead, once one is set up (run
`setup.sh` again with environment `production`).

| GitHub environment setting | Kind |
| --- | --- |
| `PROJECT_ID`, `PROJECT_NUMBER`, `REGION`, `SQL_CONNECTION_NAME`, `DB_NAME`, `DB_USERNAME`, `RUNTIME_SERVICE_ACCOUNT`, `ADMIN_USERNAME` | variables |
| `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`, `ANTHROPIC_API_KEY` (optional) | secrets |
| `REPO`, `API_SERVICE_NAME`, `FRONTEND_SERVICE_NAME`, `CORS_ALLOWED_ORIGINS` | optional variables |

## Importing recipes

Admin > Import recipes takes a `recipes.json` file (an array of recipes in the API's recipe format, each with
an optional `images` list of file names) plus the image files. Recipes whose title already exists are
skipped. Card scans and recipe bundles are kept out of this repository because it is public.
