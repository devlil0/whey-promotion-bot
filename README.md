# Whey Promotion Bot

> 🇧🇷 Português &nbsp;|&nbsp; [🇺🇸 English](README.en.md)

Bot que monitora preços de whey protein em 8 lojas brasileiras, calcula o melhor custo-benefício por grama de proteína e dispara alertas automáticos via **Telegram** e **WhatsApp** — com mensagens geradas por IA (Groq / Llama 3.3 70B).

---

## O que ele faz

- **Coleta preços 2x ao dia** (08h00 e 20h00, horário de Brasília) em 8 lojas
- **Calcula o ranking** de custo por grama de proteína — quanto menor, melhor
- **Detecta promoções automaticamente**: se um produto cai ≥ 5% abaixo da média de 7 dias, dispara alerta
- **Envia o ranking diário** às 08h05 com foto e link de cada produto
- **Envia ofertas por faixa de preço** de Growth Supplements (12h) e ProFit Labs (16h)
- **Envia oferta relâmpago** da Soldiers Nutrition (20h10)
- **Mensagens geradas por IA**: cada produto passa pelo Groq (Llama 3.3 70B) antes de ser enviado
- **API REST** protegida por API Key para consultar ranking e disparar envios manualmente

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.5 |
| HTTP Client | Spring WebFlux (WebClient) |
| Agendamento | Spring Scheduler (`@Scheduled`) |
| Banco de dados | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Geração de mensagens | Groq API (Llama 3.3 70B) |
| Notificações | Telegram Bot API + Evolution API (WhatsApp) |
| Containerização | Docker + Docker Compose |
| Deploy | Railway |

---

## Como o ranking é calculado

```
custo por grama de proteína = preço / proteína total da embalagem (g)
```

A proteína total vem de uma tabela nutricional interna (`nutrition_info`), com matching por nome do produto, marca e peso da embalagem. Quanto menor o valor, melhor o custo-benefício.

---

## Como as promoções são detectadas

A cada coleta, o serviço compara o preço atual com a **média dos últimos 7 dias** (`price_history`). Se a queda for ≥ 5% e houver pelo menos 3 amostras no período, um alerta é disparado com foto, desconto e link de compra. O threshold e a janela são configuráveis via variáveis de ambiente.

---

## Fluxo completo

```
Scheduler
    │
    ├─ 08h00 / 20h00 ──► StoreCollectorService (8 lojas)
    │                         └─ OfferPersistenceService (upsert + price_history)
    │                         └─ RankingService (calcula custo/g proteína)
    │                         └─ PromotionService (compara vs média 7 dias)
    │                               └─ GroqMessageService (gera caption via IA)
    │                                     ├─ TelegramNotificationService
    │                                     └─ EvolutionNotificationService (WhatsApp)
    │
    ├─ 08h05 ──► Ranking diário (Telegram + WhatsApp)
    ├─ 12h00 ──► Ofertas Growth por faixa de preço (Telegram + WhatsApp)
    ├─ 16h00 ──► Promoções ProFit Labs (Telegram + WhatsApp)
    └─ 20h10 ──► Oferta Relâmpago Soldiers Nutrition (Telegram + WhatsApp)
```

---

## Endpoints

Todos os endpoints exigem o header `X-API-Key`.

### Dados persistidos

```
GET  /api/health
GET  /api/products
GET  /api/products/available
GET  /api/products/by-store?store=GROWTH

GET  /api/rankings/whey/top-cost-benefit?top=10
GET  /api/rankings/whey/top-cost-benefit?top=5&store=DARK_LAB
```

### Coleta ao vivo

```
GET  /api/growth/offers
GET  /api/growth/ofertas
GET  /api/growth/ofertas/bands
GET  /api/darklab/offers
GET  /api/profitlabs/offers
GET  /api/profitlabs/promocoes
GET  /api/profitlabs/promocoes/bands
GET  /api/soldiers/offers
GET  /api/soldiers/oferta-relampago
GET  /api/blackskull/offers
GET  /api/nutrata/offers
GET  /api/adaptogen/offers
GET  /api/absolut/offers
GET  /api/offers/whey
```

### Disparo manual — Telegram

```
POST /api/telegram/trigger/ranking?top=10
POST /api/telegram/trigger/promotions
POST /api/telegram/trigger/growth-ofertas
POST /api/telegram/trigger/profitlabs-promocoes
POST /api/telegram/trigger/soldiers-relampago
```

### Disparo manual — WhatsApp (Evolution)

```
POST /api/evolution/trigger/ranking?top=10
POST /api/evolution/trigger/promotions
POST /api/evolution/trigger/growth-ofertas
POST /api/evolution/trigger/profitlabs-promocoes
POST /api/evolution/trigger/soldiers-relampago
```

---

## Rodando localmente

**Pré-requisitos:** Docker, Java 17+, Maven 3.9+

```bash
# 1. Sobe o banco
docker-compose up -d

# 2. Roda a aplicação
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. Na primeira execução o `StartupCollector` dispara uma coleta automática se o banco estiver vazio. Aguarde ~30 segundos e consulte o ranking:

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
| `API_KEY` | Chave para proteger os endpoints | *(vazio = sem proteção)* |
| `TELEGRAM_BOT_TOKEN` | Token do bot do Telegram | *(vazio = sem envio)* |
| `TELEGRAM_CHAT_ID` | ID do grupo ou canal | — |
| `EVOLUTION_API_URL` | URL base da Evolution API | `http://localhost:8081` |
| `EVOLUTION_API_KEY` | API key da Evolution | *(vazio = sem envio)* |
| `EVOLUTION_INSTANCE` | Nome da instância WhatsApp | `whey-bot` |
| `EVOLUTION_NUMBER` | Número/grupo de destino | — |
| `GROQ_API_KEY` | API key do Groq (IA) | *(vazio = usa templates fixos)* |
| `PORT` | Porta HTTP | `8080` |

---

## Deploy no Railway

1. **New Project → Deploy from GitHub repo**
2. Adicione um plugin **PostgreSQL**
3. Configure as variáveis de ambiente no painel
4. O Railway faz build e deploy automaticamente a cada push na `main`

---

## Estrutura do projeto

```
src/main/java/com/devlil0/whey_promotion_bot/
├── client/      # Clientes HTTP por loja (8 lojas)
├── config/      # WebClient, interceptor de API Key, seeder nutricional
├── controller/  # Endpoints REST, triggers Telegram e Evolution
├── dto/         # ProductOfferResponse, RankingItemResponse, PromotionAlert, etc.
├── entity/      # JPA: ProductOffer, NutritionInfo, ProductScore, PriceHistory
├── repository/  # Spring Data JPA repositories
├── scheduler/   # Coleta 2x/dia + agendamentos de envio
└── service/     # Ranking, promoções, coleta, notificações, IA (Groq), matching nutricional
```

---

## Banco de dados

| Tabela | Descrição |
|---|---|
| `product_offer` | Produtos coletados com preço, disponibilidade e URL |
| `nutrition_info` | Tabela nutricional: proteína por dose, total por embalagem |
| `product_score` | Ranking calculado: custo/g de proteína + posição |
| `price_history` | Snapshot de preço a cada coleta — base da média de 7 dias |

Schema criado e atualizado automaticamente pelo Hibernate (`ddl-auto: update`).

---

## Configuração de promoções

| Parâmetro | Padrão | Descrição |
|---|---|---|
| `promotion.discount-threshold` | `0.05` | Queda mínima em relação à média (5%) |
| `promotion.history-days` | `7` | Janela da média em dias |
| `promotion.min-history-samples` | `3` | Amostras mínimas para ativar o alerta |
