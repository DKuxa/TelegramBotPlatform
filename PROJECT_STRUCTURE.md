# 📁 TelegramBotPlatform - Project Structure

## Complete File Tree (After Webhook Integration)

```
TelegramBotPlatform/
├── 📄 .env                              ← Updated: Added MEDIA_BOT_TOKEN, MEDIA_CHANNEL_ID
├── 📄 docker-compose.yml                ← Updated: Added media bot env vars
├── 📄 Dockerfile
├── 📄 pom.xml
├── 📄 HELP.md
├── 📄 WEBHOOK_INTEGRATION_GUIDE.md      ← NEW: Complete setup guide
├── 📄 IMPLEMENTATION_SUMMARY.md         ← NEW: Technical documentation
├── 📄 PROJECT_STRUCTURE.md              ← NEW: This file
│
├── src/main/
│   ├── java/org/kuxa/telegrambotplatform/
│   │   │
│   │   ├── 📦 aop/                      [Aspect-Oriented Programming]
│   │   │   ├── GlobalExceptionAspect.java  ← Catches bot errors → AdminBot alert
│   │   │   └── LoggingAspect.java          ← Logs all bot interactions → action_log
│   │   │
│   │   ├── 📦 bots/                     [Telegram Bot Implementations]
│   │   │   ├── AdminBot.java               ← System admin bot (existing)
│   │   │   └── MediaInfoBot.java           ← NEW: Media content bot
│   │   │
│   │   ├── 📦 config/                   [Spring Configuration]
│   │   │   └── TelegramBotConfig.java      ← Bot pool initialization
│   │   │
│   │   ├── 📦 controller/               [REST API Layer]
│   │   │   └── WebhookController.java      ← NEW: Webhook endpoints
│   │   │
│   │   ├── 📦 core/                     [Core Bot Framework]
│   │   │   ├── BaseTelegramBot.java        ← Updated: Added getTelegramClient()
│   │   │   └── BotRegistry.java            ← Bot lifecycle manager
│   │   │
│   │   ├── 📦 domain/                   [JPA Entities]
│   │   │   ├── ActionLog.java              ← User interaction logs
│   │   │   ├── AppUser.java                ← User state management
│   │   │   ├── ErrorLog.java               ← Error tracking
│   │   │   └── WebhookEvent.java           ← NEW: Webhook audit trail
│   │   │
│   │   ├── 📦 dto/                      [Data Transfer Objects]
│   │   │   └── webhook/
│   │   │       ├── RadarrWebhookDTO.java   ← NEW: Radarr webhook structure
│   │   │       ├── SonarrWebhookDTO.java   ← NEW: Sonarr webhook structure
│   │   │       └── TrueNasAlertDTO.java    ← NEW: TrueNAS alert structure
│   │   │
│   │   ├── 📦 repository/               [Spring Data JPA Repositories]
│   │   │   ├── ActionLogRepository.java    ← Action log queries
│   │   │   ├── ErrorLogRepository.java     ← Error log queries
│   │   │   └── WebhookEventRepository.java ← NEW: Webhook event queries
│   │   │
│   │   ├── 📦 service/                  [Business Logic Layer]
│   │   │   ├── MediaNotificationService.java     ← NEW: Message formatting
│   │   │   └── WebhookRoutingService.java        ← NEW: Smart router
│   │   │
│   │   └── TelegramBotPlatformApplication.java   ← Main Spring Boot app
│   │
│   └── resources/
│       ├── application.yaml             ← Updated: Added media bot config
│       └── db/migration/                [Flyway Migrations]
│           ├── V1__init.sql            ← Initial schema (existing)
│           └── V2__webhook_events.sql  ← NEW: webhook_event table
│
└── .git/
```

---

## 📊 Component Relationships

### Data Flow Diagram

```
External System          REST API              Service Layer           Bot Layer            Telegram
─────────────────────────────────────────────────────────────────────────────────────────────────────

   Radarr/Sonarr    →   WebhookController   →  WebhookRoutingService  →  MediaInfoBot    →  Public Channel
   (POST JSON)          /api/webhooks/*         • Parse JSON              • Format msg        (@your_channel)
                                                • Try/catch               • Send with poster
                                                • Audit log
                                                   ↓
                                             MediaNotificationService
                                                • Format message
                                                • Route to bot


   TrueNAS Alert    →   WebhookController   →  WebhookRoutingService  →  AdminBot        →  Private Chat
   (POST JSON)          /api/webhooks/*         • Parse JSON              • Format alert      (@admin_user)
                                                • Try/catch               • Send immediately
                                                • Audit log
                                                   ↓
                                             MediaNotificationService
                                                • Format alert
                                                • Route to admin


   All webhooks     →   Database (PostgreSQL)
                        • webhook_event (audit trail)
                        • action_log (bot interactions)
                        • error_log (exceptions)
```

---

## 🎯 Key Components by Responsibility

### 1️⃣ Entry Points (REST API)

| File | Endpoints | Purpose |
|------|-----------|---------|
| `WebhookController.java` | `/api/webhooks/radarr`<br>`/api/webhooks/sonarr`<br>`/api/webhooks/truenas`<br>`/api/webhooks/health` | Receive webhooks from external systems |

### 2️⃣ Business Logic (Services)

| File | Methods | Purpose |
|------|---------|---------|
| `WebhookRoutingService.java` | `handleRadarrWebhook()`<br>`handleSonarrWebhook()`<br>`handleTrueNasWebhook()` | Parse JSON, route to correct handler, log to DB |
| `MediaNotificationService.java` | `processRadarrEvent()`<br>`processSonarrEvent()`<br>`processTrueNasAlert()` | Format messages, call bot methods |

### 3️⃣ Data Layer (Entities & Repositories)

| Entity | Repository | Purpose |
|--------|------------|---------|
| `WebhookEvent` | `WebhookEventRepository` | Audit all incoming webhooks |
| `ActionLog` | `ActionLogRepository` | Log bot user interactions |
| `ErrorLog` | `ErrorLogRepository` | Log bot exceptions |
| `AppUser` | *(not shown)* | User state management |

### 4️⃣ Telegram Integration (Bots)

| Bot | Purpose | Target |
|-----|---------|--------|
| `MediaInfoBot` | Post media content (movies/TV shows) | Public channel |
| `AdminBot` | System alerts & admin commands | Private admin chat |

### 5️⃣ Data Transfer (DTOs)

| DTO | Source | Maps |
|-----|--------|------|
| `RadarrWebhookDTO` | Radarr | Movie events (Grab/Download) |
| `SonarrWebhookDTO` | Sonarr | TV series events |
| `TrueNasAlertDTO` | TrueNAS | System alerts (CRITICAL/WARNING) |

### 6️⃣ Cross-Cutting Concerns (AOP)

| Aspect | Trigger | Action |
|--------|---------|--------|
| `LoggingAspect` | Any bot.consume() call | Log to action_log table |
| `GlobalExceptionAspect` | Any bot exception | Log to error_log + alert admin |

---

## 🗂️ Database Schema

```sql
┌─────────────────────┐
│    webhook_event    │  ← NEW TABLE
├─────────────────────┤
│ id (PK)             │
│ source              │  radarr/sonarr/truenas
│ event_type          │  Grab/Download/CRITICAL
│ payload (TEXT)      │  Raw JSON for debugging
│ processed_success   │  true/false
│ error_message       │  Exception details
│ routed_to_bot       │  MediaInfoBot/AdminBot
│ target_channel_id   │  Telegram chat ID
│ created_at          │
└─────────────────────┘
         ↓
    [Indexes on: source, created_at, processed_successfully]

┌─────────────────────┐
│     action_log      │  ← EXISTING TABLE
├─────────────────────┤
│ id (PK)             │
│ bot_name            │
│ chat_id             │
│ message_text        │
│ created_at          │
└─────────────────────┘

┌─────────────────────┐
│      error_log      │  ← EXISTING TABLE
├─────────────────────┤
│ id (PK)             │
│ bot_name            │
│ error_message       │
│ stack_trace         │
│ created_at          │
└─────────────────────┘

┌─────────────────────┐
│      app_user       │  ← EXISTING TABLE
├─────────────────────┤
│ id (PK)             │
│ chat_id (UNIQUE)    │
│ username            │
│ state               │
│ created_at          │
└─────────────────────┘
```

---

## 🔍 Code Organization Patterns

### Layered Architecture

```
┌───────────────────────────────────────────┐
│          Presentation Layer               │
│  @RestController (WebhookController)      │
└───────────────────┬───────────────────────┘
                    ↓
┌───────────────────────────────────────────┐
│          Service Layer                    │
│  @Service (WebhookRoutingService)         │
│  @Service (MediaNotificationService)      │
└───────────────────┬───────────────────────┘
                    ↓
┌───────────────────────────────────────────┐
│          Domain Layer                     │
│  @Component (MediaInfoBot)                │
│  @Component (AdminBot)                    │
└───────────────────┬───────────────────────┘
                    ↓
┌───────────────────────────────────────────┐
│          Data Access Layer                │
│  @Repository (WebhookEventRepository)     │
│  @Repository (ActionLogRepository)        │
│  @Repository (ErrorLogRepository)         │
└───────────────────┬───────────────────────┘
                    ↓
┌───────────────────────────────────────────┐
│          Database (PostgreSQL)            │
│  Tables: webhook_event, action_log, etc   │
└───────────────────────────────────────────┘
```

### Dependency Injection Flow

```
Spring Boot Application
    ↓
BotRegistry (@Service)
    ↓ injects List<BaseTelegramBot>
    ├── AdminBot (@Component)
    └── MediaInfoBot (@Component)

WebhookController (@RestController)
    ↓ injects
WebhookRoutingService (@Service)
    ↓ injects
    ├── MediaNotificationService (@Service)
    │       ↓ injects
    │       ├── MediaInfoBot
    │       └── AdminBot
    └── WebhookEventRepository (@Repository)
```

---

## 📦 Docker Container Structure

```
Docker Network: telegram_bot_network
├── Container: multibot_postgres
│   ├── Image: postgres:15-alpine
│   ├── Port: 5432:5432
│   └── Volume: pgdata
│
└── Container: multibot_app
    ├── Image: telegram-bot-platform:latest
    ├── Port: 8080:8080
    ├── Depends on: multibot_postgres
    └── Environment:
        ├── DB_URL=jdbc:postgresql://db:5432/multibot
        ├── ADMIN_BOT_TOKEN=...
        ├── ADMIN_CHAT_ID=...
        ├── MEDIA_BOT_TOKEN=...        ← NEW
        ├── MEDIA_CHANNEL_ID=...       ← NEW
        └── TMDB_API_KEY=...           ← NEW
```

---

## 🔗 External System Integration Points

```
┌──────────────────┐
│  Radarr/Sonarr   │  Settings → Connect → Webhook
│  (Port 7878/8989)│
└────────┬─────────┘
         │ POST http://multibot_app:8080/api/webhooks/radarr
         ↓
┌──────────────────────────────┐
│  TelegramBotPlatform         │
│  (Port 8080)                 │
│  Container: multibot_app     │
└────────┬─────────────────────┘
         │
         ↓
┌──────────────────┐
│  Telegram API    │
│  (External)      │
│  • MediaInfoBot  │  → Public Channel
│  • AdminBot      │  → Private Chat
└──────────────────┘

┌──────────────────┐
│  TrueNAS Scale   │  System Settings → Alert Settings
│  (Host OS)       │
└────────┬─────────┘
         │ POST http://192.168.1.X:8080/api/webhooks/truenas
         ↓
┌──────────────────────────────┐
│  TelegramBotPlatform         │
│  (Same as above)             │
└──────────────────────────────┘
```

---

## 🚀 Deployment Flow

```
1. Development
   └── Local IDE (IntelliJ IDEA)
       └── Maven build: mvn clean package

2. Containerization
   └── Dockerfile
       └── FROM eclipse-temurin:21-jre-alpine
       └── COPY target/*.jar app.jar

3. Orchestration
   └── docker-compose.yml
       ├── Service: db (PostgreSQL)
       └── Service: app (Spring Boot)
           └── Depends on: db
           └── Flyway auto-migration on startup

4. Runtime
   └── Docker containers on TrueNAS Scale
       └── Network: telegram_bot_network
       └── Persistent volume: pgdata
       └── Logs: docker logs multibot_app
```

---

## 📝 Configuration Files

| File | Purpose | Location |
|------|---------|----------|
| `application.yaml` | Spring Boot config | `src/main/resources/` |
| `.env` | Environment variables (secrets) | Project root |
| `docker-compose.yml` | Container orchestration | Project root |
| `Dockerfile` | Container image definition | Project root |
| `pom.xml` | Maven dependencies | Project root |
| `V1__init.sql` | Initial DB schema | `src/main/resources/db/migration/` |
| `V2__webhook_events.sql` | Webhook table schema | `src/main/resources/db/migration/` |

---

## 🎓 Code Style & Conventions

### 1. Naming Conventions
- **DTOs**: `*DTO.java` (e.g., `RadarrWebhookDTO`)
- **Entities**: Simple nouns (e.g., `WebhookEvent`)
- **Services**: `*Service.java` (e.g., `WebhookRoutingService`)
- **Controllers**: `*Controller.java` (e.g., `WebhookController`)
- **Repositories**: `*Repository.java` (e.g., `WebhookEventRepository`)

### 2. Annotations
- `@Component` - Telegram bots
- `@Service` - Business logic
- `@RestController` - REST endpoints
- `@Repository` - Data access
- `@Entity` - JPA entities

### 3. Lombok Usage
- `@Data` - DTOs (auto-generates getters/setters/toString)
- `@Builder` - Entities (fluent builder pattern)
- `@RequiredArgsConstructor` - Services (DI via constructor)
- `@Slf4j` - Logging

### 4. Comments
- Russian for business logic comments (matching existing code)
- Javadoc for public APIs
- Inline comments for complex algorithms

---

## 📈 Monitoring & Observability

### Log Levels
```
root: INFO
org.kuxa.telegrambotplatform.core: DEBUG
org.kuxa.telegrambotplatform.aop: TRACE
org.springframework.web: INFO
org.hibernate.SQL: DEBUG
```

### Key Log Patterns
```bash
# Bot registration
"Бот [MediaInfoBot] успешно зарегистрирован"

# Webhook received
"📥 Получен Radarr webhook. Размер payload: X полей"

# Webhook processed
"✅ Radarr webhook успешно обработан: Download"

# Webhook failed
"❌ Ошибка обработки webhook от radarr: ..."
```

### Database Queries for Monitoring
```sql
-- Daily webhook statistics
SELECT source, COUNT(*) as total
FROM webhook_event
WHERE DATE(created_at) = CURRENT_DATE
GROUP BY source;

-- Failed webhooks in last hour
SELECT * FROM webhook_event
WHERE processed_successfully = false
  AND created_at >= NOW() - INTERVAL '1 hour';
```

---

**Last Updated**: 2024-02-15
**Architecture Version**: 2.0 (With Webhook Integration)
**Total Files**: 21 Java files + 3 documentation files
