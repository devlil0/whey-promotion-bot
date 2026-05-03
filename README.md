# Whey Promotion Bot

Bot que monitora preços de whey protein em lojas brasileiras e no Mercado Livre, calcula o melhor custo-benefício por grama de proteína e envia alertas automáticos de promoção e ranking diário para um grupo do Telegram.

---

## O que ele faz

- **Coleta preços 2x ao dia** (08h e 20h, horário de Brasília) em 4 fontes: Growth Supplements, Dark Lab, ProFit Labs e Mercado Livre
- **Calcula o ranking** de custo por grama de proteína em tempo real — quanto menor, melhor
- **Detecta promoções automaticamente**: se um produto cai ≥ 15% abaixo da média móvel de 7 dias, dispara alerta no Telegram
- **Envia o ranking diário** às 08h05 com foto de cada produto e link direto para compra
- **API REST** para consultar ranking e ofertas ao vivo

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.5 |
| HTTP Client | Spring WebFlux (WebClient) |
| Agendamento | Spring Scheduler (`@Scheduled`) |
| Banco de dados | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Containerização | Docker + Docker Compose |
| Deploy | Railway |

---

## Fontes de dados

| Loja | Plataforma | Endpoint |
|---|---|---|
| Growth Supplements | API interna (wapstore) | `/api/v2/produtos` |
| Dark Lab | Shopify (`/products.json`) | Paginação automática |
| ProFit Labs | Tray Commerce (`/web_api/products`) | Filtro por categoria |
| Mercado Livre | API pública (`/sites/MLB/search`) | `q=whey protein` |

---

## Como o ranking é calculado

```
custo por grama de proteína = preço / proteína total da embalagem (g)
```

A proteína total é obtida via tabela nutricional interna (`nutrition_info`), com matching pelo nome do produto, marca e peso da embalagem. Quanto menor o valor, melhor o custo-benefício.

---

## Como as promoções são detectadas

A cada coleta, o serviço compara o preço atual com a **média móvel dos últimos 7 dias** do histórico de preços (`price_history`). Se a queda for ≥ 15% e houver pelo menos 3 amostras no período, um alerta é disparado no Telegram com foto do produto, desconto e link de compra.

O threshold e o janela de histórico são configuráveis via variáveis de ambiente.

---

## Fluxo completo

```
Scheduler (08:00 e 20:00 BRT)
    │
    ├─ StoreCollectorService ──► 4 lojas em paralelo
    │
    ├─ OfferPersistenceService ──► upsert em product_offer + snapshot em price_history
    │
    ├─ RankingService ──► calcula custo/g proteína, persiste em product_score
    │
    └─ PromotionService ──► compara preço vs média 7 dias
            │
            └─ TelegramNotificationService
                    ├─ /sendPhoto por promoção detectada
                    └─ /sendPhoto por item do ranking (08:05 BRT)
```

---

## Endpoints

### Banco de dados

```
GET /api/health
GET /api/products
GET /api/products/available
GET /api/products/by-store?store=GROWTH

GET /api/rankings/whey/top-cost-benefit?top=10
GET /api/rankings/whey/top-cost-benefit?top=5&store=MERCADO_LIVRE
```

### Coleta ao vivo (busca nas APIs das lojas na hora)

```
GET /api/growth/offers
GET /api/darklab/offers
GET /api/profitlabs/offers
GET /api/offers/whey

GET /api/growth/category/raw?category=/whey-protein/&offset=0&limit=30
GET /api/darklab/products/raw?page=1&limit=250
GET /api/profitlabs/products/raw?page=1&limit=50
```

### Disparo manual do Telegram

```
POST /api/telegram/trigger/ranking?top=10
POST /api/telegram/trigger/promotions
```

---

## Rodando localmente

**Pré-requisitos:** Docker, Java 17+, Maven 3.9+

```bash
# 1. Sobe o banco
docker-compose up -d postgres

# 2. Roda a aplicação
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.

Na primeira execução o `StartupCollector` dispara uma coleta automática se o banco estiver vazio. Aguarde ~30 segundos e consulte o ranking:

```
GET http://localhost:8080/api/rankings/whey/top-cost-benefit?top=10
```

---

## Variáveis de ambiente

| Variável | Descrição | Padrão |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL do PostgreSQL | `jdbc:postgresql://localhost:5432/whey_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `whey_user` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `whey_pass` |
| `GROWTH_API_APP_TOKEN` | Token da API da Growth | `wapstore` |
| `TELEGRAM_BOT_TOKEN` | Token do bot do Telegram | *(vazio = sem envio)* |
| `TELEGRAM_CHAT_ID` | ID do grupo ou canal | — |
| `PORT` | Porta HTTP da aplicação | `8080` |

Copie `.env.example` para `.env` e preencha os valores antes de rodar localmente.

---

## Deploy no Railway

O projeto inclui `railway.toml` configurado com Dockerfile builder, healthcheck em `/api/health` e restart automático.

**Passos:**

1. No Railway: **New Project → Deploy from GitHub repo**
2. Adicione um plugin **PostgreSQL**
3. No serviço da aplicação, configure as variáveis usando referências do Railway:

```
SPRING_DATASOURCE_URL      = jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME = ${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD = ${{Postgres.PGPASSWORD}}
GROWTH_API_APP_TOKEN       = wapstore
TELEGRAM_BOT_TOKEN         = <token do bot>
TELEGRAM_CHAT_ID           = <id do grupo>
```

4. O Railway faz o build e deploy automaticamente a cada push na `main`

---

## Estrutura do projeto

```
src/main/java/com/devlil0/whey_promotion_bot/
├── client/          # Clientes HTTP por loja (Growth, Dark Lab, ProFit Labs, ML)
├── config/          # WebClient, seeder de dados nutricionais
├── controller/      # Endpoints REST + trigger manual do Telegram
├── dto/             # Records: ProductOfferResponse, RankingItemResponse, PromotionAlert
├── entity/          # JPA: ProductOffer, NutritionInfo, ProductScore, PriceHistory
├── repository/      # Spring Data JPA repositories
├── scheduler/       # Coleta 2x/dia + ranking diário às 08:05
└── service/         # Lógica de coleta, ranking, promoções, Telegram, matching nutricional
```

---

## Banco de dados

| Tabela | Descrição |
|---|---|
| `product_offer` | Produtos coletados das lojas com preço, disponibilidade e URL |
| `nutrition_info` | Tabela nutricional manual: proteína por dose, doses por embalagem |
| `product_score` | Ranking calculado: custo por grama de proteína + posição |
| `price_history` | Snapshot de preço a cada coleta — base para a média móvel de 7 dias |

O schema é criado e atualizado automaticamente pelo Hibernate (`ddl-auto: update`).

---

## Configuração de promoções

Ajustável via variáveis de ambiente ou diretamente no `application.yml`:

| Parâmetro | Padrão | Descrição |
|---|---|---|
| `promotion.discount-threshold` | `0.15` | Queda mínima em relação à média (15%) |
| `promotion.history-days` | `7` | Janela da média móvel em dias |
| `promotion.min-history-samples` | `3` | Amostras mínimas para ativar o alerta |
