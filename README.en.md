# Whey Promotion Bot

> [🇧🇷 Português](README.md) &nbsp;|&nbsp; 🇺🇸 English

A bot that monitors whey protein prices across Brazilian online stores and Mercado Livre, calculates the best cost-per-gram of protein, and automatically sends promotion alerts and a daily ranking to a Telegram group.

---

## What it does

- **Collects prices twice a day** (8 AM and 8 PM, Brasília time) from 4 sources: Growth Supplements, Dark Lab, ProFit Labs, and Mercado Livre
- **Calculates a real-time ranking** by cost per gram of protein — the lower, the better
- **Automatically detects promotions**: if a product drops ≥ 15% below its 7-day moving average, an alert is sent to Telegram with the product photo and a direct purchase link
- **Sends a daily ranking** at 8:05 AM with each product's photo and store link
- **REST API** to query the ranking and live offers

---

## Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| HTTP Client | Spring WebFlux (WebClient) |
| Scheduling | Spring Scheduler (`@Scheduled`) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Containerization | Docker + Docker Compose |
| Deployment | Railway |

---

## Data sources

| Store | Platform | Endpoint |
|---|---|---|
| Growth Supplements | Internal API (wapstore) | `/api/v2/produtos` |
| Dark Lab | Shopify (`/products.json`) | Auto pagination |
| ProFit Labs | Tray Commerce (`/web_api/products`) | Category filter |
| Mercado Livre | Public API (`/sites/MLB/search`) | `q=whey protein` |

---

## How the ranking is calculated

```
cost per gram of protein = price / total protein in package (g)
```

Total protein is sourced from an internal nutrition table (`nutrition_info`), matched by product name, brand, and package weight. The lower the value, the better the deal.

---

## How promotions are detected

On every collection run, the service compares the current price against the **7-day moving average** from the price history (`price_history`). If the drop is ≥ 15% and there are at least 3 samples in the window, a Telegram alert is fired with the product photo, discount percentage, and purchase link.

The threshold and history window are configurable via environment variables.

---

## Full flow

```
Scheduler (8:00 AM and 8:00 PM BRT)
    │
    ├─ StoreCollectorService ──► 4 stores in parallel
    │
    ├─ OfferPersistenceService ──► upsert into product_offer + snapshot into price_history
    │
    ├─ RankingService ──► calculate cost/g protein, persist into product_score
    │
    └─ PromotionService ──► compare price vs 7-day average
            │
            └─ TelegramNotificationService
                    ├─ /sendPhoto per detected promotion
                    └─ /sendPhoto per ranking item (8:05 AM BRT)
```

---

## Endpoints

### Database (persisted data)

```
GET /api/health
GET /api/products
GET /api/products/available
GET /api/products/by-store?store=GROWTH

GET /api/rankings/whey/top-cost-benefit?top=10
GET /api/rankings/whey/top-cost-benefit?top=5&store=MERCADO_LIVRE
```

### Live collection (fetches from store APIs on demand)

```
GET /api/growth/offers
GET /api/darklab/offers
GET /api/profitlabs/offers
GET /api/offers/whey

GET /api/growth/category/raw?category=/whey-protein/&offset=0&limit=30
GET /api/darklab/products/raw?page=1&limit=250
GET /api/profitlabs/products/raw?page=1&limit=50
```

### Manual Telegram trigger

```
POST /api/telegram/trigger/ranking?top=10
POST /api/telegram/trigger/promotions
```

---

## Running locally

**Requirements:** Docker, Java 17+, Maven 3.9+

```bash
# 1. Start the database
docker-compose up -d postgres

# 2. Run the application
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

On the first run, `StartupCollector` automatically triggers a data collection if the database is empty. Wait ~30 seconds and query the ranking:

```
GET http://localhost:8080/api/rankings/whey/top-cost-benefit?top=10
```

---

## Environment variables

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/whey_db` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `whey_user` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `whey_pass` |
| `GROWTH_API_APP_TOKEN` | Growth Supplements API token | `wapstore` |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token | *(empty = no sending)* |
| `TELEGRAM_CHAT_ID` | Target group or channel ID | — |
| `PORT` | HTTP port | `8080` |

Copy `.env.example` to `.env` and fill in the values before running locally.

---

## Deploying to Railway

The project includes a `railway.toml` configured with Dockerfile builder, healthcheck at `/api/health`, and automatic restart policy.

**Steps:**

1. On Railway: **New Project → Deploy from GitHub repo**
2. Add a **PostgreSQL** plugin
3. In the application service, set the variables using Railway's reference syntax:

```
SPRING_DATASOURCE_URL      = jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME = ${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD = ${{Postgres.PGPASSWORD}}
GROWTH_API_APP_TOKEN       = wapstore
TELEGRAM_BOT_TOKEN         = <your bot token>
TELEGRAM_CHAT_ID           = <your group id>
```

4. Railway builds and deploys automatically on every push to `main`

---

## Project structure

```
src/main/java/com/devlil0/whey_promotion_bot/
├── client/          # HTTP clients per store (Growth, Dark Lab, ProFit Labs, ML)
├── config/          # WebClient config, nutrition data seeder
├── controller/      # REST endpoints + manual Telegram trigger
├── dto/             # Records: ProductOfferResponse, RankingItemResponse, PromotionAlert
├── entity/          # JPA: ProductOffer, NutritionInfo, ProductScore, PriceHistory
├── repository/      # Spring Data JPA repositories
├── scheduler/       # Twice-daily collection + daily ranking at 8:05 AM
└── service/         # Collection, ranking, promotion, Telegram, nutrition matching logic
```

---

## Database schema

| Table | Description |
|---|---|
| `product_offer` | Products collected from stores with price, availability, and URL |
| `nutrition_info` | Manual nutrition table: protein per serving, servings per container |
| `product_score` | Calculated ranking: cost per gram of protein + position |
| `price_history` | Price snapshot on every collection run — basis for the 7-day moving average |

The schema is created and updated automatically by Hibernate (`ddl-auto: update`).

---

## Promotion settings

Configurable via environment variables or directly in `application.yml`:

| Parameter | Default | Description |
|---|---|---|
| `promotion.discount-threshold` | `0.15` | Minimum drop relative to the average (15%) |
| `promotion.history-days` | `7` | Moving average window in days |
| `promotion.min-history-samples` | `3` | Minimum samples required to trigger an alert |

---

## Built with AI assistance

This project was developed with support from **generative artificial intelligence** to accelerate architectural decisions, code generation, and performance optimization. AI assistance enabled faster iteration on areas such as persistence layer design, nutrition matching logic, and Telegram message formatting — while keeping all technical and product decisions entirely human-driven.
