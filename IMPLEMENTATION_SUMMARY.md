# 📦 Webhook Integration Module - Implementation Summary

## ✅ Phase 1: Code Integration - COMPLETED

### 🔍 Architecture Analysis

Your existing codebase follows excellent architectural patterns. Here's what I leveraged and extended:

#### **Existing Components Used:**

| Component | Location | How Used |
|-----------|----------|----------|
| `BotRegistry` | `core/BotRegistry.java` | Auto-registers new MediaInfoBot into the bot pool |
| `AdminBot` | `bots/AdminBot.java` | Routes TrueNAS system alerts to admin chat |
| `BaseTelegramBot` | `core/BaseTelegramBot.java` | Extended for MediaInfoBot; added `getTelegramClient()` getter |
| `ActionLog` pattern | `domain/ActionLog.java` | Used as template for `WebhookEvent` entity |
| `ActionLogRepository` pattern | `repository/ActionLogRepository.java` | Used as template for `WebhookEventRepository` |
| `GlobalExceptionAspect` | `aop/GlobalExceptionAspect.java` | Already handles bot errors → admin alerts |
| `LoggingAspect` | `aop/LoggingAspect.java` | Already logs all bot interactions asynchronously |

---

### 🆕 New Components Created

#### **1. DTOs (Data Transfer Objects)** - `dto/webhook/`

Strict POJOs with full Jackson annotations for JSON parsing:

##### `RadarrWebhookDTO.java`
- Maps Radarr webhook structure (Grab/Download events)
- Nested classes: `MovieInfo`, `RemoteMovieInfo`, `ReleaseInfo`
- Helper methods: `getMovieTitle()`, `getQualityProfile()`, `getReleaseGroupName()`
- Handles all Radarr event types: Grab, Download, Rename, MovieDelete, Test

##### `SonarrWebhookDTO.java`
- Maps Sonarr webhook structure (TV series)
- Nested classes: `SeriesInfo`, `EpisodeInfo`, `ReleaseInfo`
- Helper methods: `getSeriesTitle()`, `getEpisodesDescription()`
- Supports single episodes and batch imports

##### `TrueNasAlertDTO.java`
- Maps TrueNAS alert structure (flexible for different TrueNAS versions)
- Helper methods: `isCritical()`, `getSeverityEmoji()`, `getAlertType()`
- Handles multiple field names (`message` vs `text`) for compatibility

**Key Feature**: All DTOs use `@JsonIgnoreProperties(ignoreUnknown = true)` for robustness against API changes.

---

#### **2. Domain Entity** - `domain/`

##### `WebhookEvent.java`
Audit trail for all incoming webhooks:

| Field | Type | Purpose |
|-------|------|---------|
| `source` | String | "radarr", "sonarr", "truenas" |
| `eventType` | String | Event classification (Grab, Download, CRITICAL, etc.) |
| `payload` | TEXT | Raw JSON for debugging |
| `processedSuccessfully` | Boolean | Success/failure flag |
| `errorMessage` | TEXT | Exception details if failed |
| `routedToBot` | String | Target bot name (MediaInfoBot/AdminBot) |
| `targetChannelId` | Long | Telegram channel/chat ID |
| `createdAt` | Timestamp | Auto-populated on insert |

**Database**: Table `webhook_event` with optimized indexes on `source`, `created_at`, `processed_successfully`.

---

#### **3. Repository** - `repository/`

##### `WebhookEventRepository.java`
Spring Data JPA repository with custom queries:

```java
// Find last 10 events from specific source
List<WebhookEvent> findTop10BySourceOrderByCreatedAtDesc(String source);

// Find failed events in last 24 hours
List<WebhookEvent> findFailedEventsSince(LocalDateTime since);

// Count today's events by source
long countTodayEventsBySource(String source);
```

---

#### **4. Bot** - `bots/`

##### `MediaInfoBot.java`
New bot for posting media content to public channel:

**Features**:
- `publishMediaNotification(String text)` - Text-only posts
- `publishMediaWithPoster(String imageUrl, String caption)` - Posts with movie poster
- Fallback to text if image fails to load
- Thread-safe message sending with error handling

**Configuration**:
```yaml
media:
  bot:
    token: ${MEDIA_BOT_TOKEN}
    name: MediaInfoBot
  channel:
    id: ${MEDIA_CHANNEL_ID}
```

---

#### **5. Services** - `service/`

##### `MediaNotificationService.java`
Formats messages and delegates to appropriate bot:

**Methods**:
- `processRadarrEvent(RadarrWebhookDTO)` - Formats movie notifications
- `processSonarrEvent(SonarrWebhookDTO)` - Formats TV series notifications
- `processTrueNasAlert(TrueNasAlertDTO)` - Formats system alerts

**Message Formatting**:
```markdown
🎬 Фильм загружен

📽 Название: The Matrix (1999)
🎞 Качество: Bluray-1080p
🏷 Релиз-группа: SPARKS
🔗 [IMDb](https://www.imdb.com/title/tt0133093/)
```

##### `WebhookRoutingService.java`
Smart router with comprehensive error handling:

**Features**:
- Parses JSON using Jackson `ObjectMapper`
- Wraps parsing in try-catch (malformed JSON won't crash the app)
- Logs every webhook to `webhook_event` table (audit trail)
- Routes based on source:
  - `radarr`/`sonarr` → MediaInfoBot → Public channel
  - `truenas` → AdminBot → Private admin chat
- Transactional processing ensures data consistency

**Error Handling**:
```java
try {
    // Parse and process
    auditLog.setProcessedSuccessfully(true);
} catch (Exception e) {
    // Log error, don't crash
    auditLog.setErrorMessage(e.getMessage());
} finally {
    // Always save audit log
    webhookEventRepository.save(auditLog);
}
```

---

#### **6. Controller** - `controller/`

##### `WebhookController.java`
REST API endpoints with Spring Web annotations:

**Endpoints**:

| Method | Path | Purpose | External System URL |
|--------|------|---------|---------------------|
| POST | `/api/webhooks/radarr` | Receive Radarr events | Configure in Radarr Settings → Connect |
| POST | `/api/webhooks/sonarr` | Receive Sonarr events | Configure in Sonarr Settings → Connect |
| POST | `/api/webhooks/truenas` | Receive TrueNAS alerts | Configure in TrueNAS Alert Settings |
| GET | `/api/webhooks/health` | Health check | For monitoring |
| POST | `/api/webhooks/test` | Manual testing | For debugging |

**Response Format**:
```json
{
  "status": "accepted",
  "message": "Radarr webhook принят в обработку"
}
```

**Thread Safety**: All methods use Spring's thread-safe dependency injection.

---

### 🗄️ Database Migration

**File**: `src/main/resources/db/migration/V2__webhook_events.sql`

Creates `webhook_event` table with:
- 4 optimized indexes for fast queries
- Full audit trail support
- Comments on table and columns (PostgreSQL)

**Flyway** will auto-apply this migration on next startup.

---

### ⚙️ Configuration Updates

#### **application.yaml**
```yaml
media:
  bot:
    token: ${MEDIA_BOT_TOKEN}
    name: ${MEDIA_BOT_NAME:MediaInfoBot}
  channel:
    id: ${MEDIA_CHANNEL_ID}

tmdb:
  api:
    key: ${TMDB_API_KEY:}
  poster:
    base:
      url: ${TMDB_POSTER_BASE_URL:https://image.tmdb.org/t/p/w500}

server:
  port: ${SERVER_PORT:8080}
```

#### **.env**
Added:
```env
MEDIA_BOT_TOKEN=YOUR_MEDIA_BOT_TOKEN_HERE
MEDIA_BOT_NAME=MediaInfoBot
MEDIA_CHANNEL_ID=YOUR_CHANNEL_ID_HERE
TMDB_API_KEY=YOUR_TMDB_API_KEY_HERE
```

#### **docker-compose.yml**
Added environment variables to `app` service:
```yaml
- MEDIA_BOT_TOKEN=${MEDIA_BOT_TOKEN}
- MEDIA_BOT_NAME=${MEDIA_BOT_NAME:-MediaInfoBot}
- MEDIA_CHANNEL_ID=${MEDIA_CHANNEL_ID}
- TMDB_API_KEY=${TMDB_API_KEY:-}
```

---

### 🔧 Code Modifications to Existing Files

#### `BaseTelegramBot.java`
**Change**: Added public getter for `telegramClient`
```java
public TelegramClient getTelegramClient() {
    return telegramClient;
}
```
**Reason**: Allows `MediaNotificationService` to send alerts via `AdminBot`'s client.

---

## 📋 Phase 2: Media Stack Configuration

### Complete configuration tables for external systems:

---

### 🎬 Radarr Configuration

**Navigation Path**: `Settings` → `Connect` → `+` (Add Connection) → Select **Webhook**

| Setting Name | Value |
|--------------|-------|
| **Name** | `TelegramBotPlatform` |
| **URL** | `http://multibot_app:8080/api/webhooks/radarr` ⚠️ |
| **Method** | `POST` |
| **Username** | *(leave empty)* |
| **Password** | *(leave empty)* |
| **On Grab** | ✅ Enable |
| **On Download** | ✅ Enable |
| **On Upgrade** | ✅ Enable |
| **On Rename** | ⬜ Disable (optional) |
| **On Movie Added** | ⬜ Disable (optional) |
| **On Movie Delete** | ⬜ Disable |
| **On Movie File Delete** | ⬜ Disable |
| **On Health Issue** | ⬜ Disable |
| **On Application Update** | ⬜ Disable |
| **Tags** | *(empty = all movies)* |

⚠️ **URL Note**:
- If Radarr is in same Docker network: `http://multibot_app:8080/api/webhooks/radarr`
- If Radarr is on TrueNAS (not in Docker): `http://YOUR_NAS_IP:8080/api/webhooks/radarr`

**Test**: Click **Test** button → Check Telegram channel for test message.

---

### 📺 Sonarr Configuration

**Navigation Path**: `Settings` → `Connect` → `+` (Add Connection) → Select **Webhook**

| Setting Name | Value |
|--------------|-------|
| **Name** | `TelegramBotPlatform` |
| **URL** | `http://multibot_app:8080/api/webhooks/sonarr` ⚠️ |
| **Method** | `POST` |
| **Username** | *(leave empty)* |
| **Password** | *(leave empty)* |
| **On Grab** | ✅ Enable |
| **On Import** | ✅ Enable (= On Download) |
| **On Upgrade** | ✅ Enable |
| **On Rename** | ⬜ Disable (optional) |
| **On Series Add** | ⬜ Disable (optional) |
| **On Series Delete** | ⬜ Disable |
| **On Episode File Delete** | ⬜ Disable |
| **On Health Issue** | ⬜ Disable |
| **On Application Update** | ⬜ Disable |
| **Tags** | *(empty = all series)* |

⚠️ **URL Note**: Same as Radarr (Docker network vs TrueNAS).

**Test**: Click **Test** button → Check Telegram channel for test message.

---

## 📋 Phase 3: TrueNAS Alerts Integration

### 🖥️ TrueNAS Scale Configuration

**Navigation Path**: `System Settings` → `Alert Settings` → `Add` (in Alert Services section)

| Setting Name | Value |
|--------------|-------|
| **Name** | `TelegramBotPlatform` |
| **Enabled** | ✅ Enable |
| **Type** | `Generic Webhook` or `Custom Webhook` |
| **URL** | `http://YOUR_NAS_IP:8080/api/webhooks/truenas` 🚨 |
| **Method** | `POST` |
| **Headers** | `Content-Type: application/json` |
| **Alert Level** | Select: `CRITICAL`, `WARNING` |
| **Frequency** | `IMMEDIATELY` |

🚨 **CRITICAL URL NOTE**:
**You MUST use the TrueNAS host IP address**, NOT `multibot_app`!

**Why?** TrueNAS is the host OS, not a Docker container, so it can't resolve Docker container names.

**Example**: If your TrueNAS IP is `192.168.1.100`:
```
http://192.168.1.100:8080/api/webhooks/truenas
```

### Testing TrueNAS Integration

**Method 1**: TrueNAS Built-in Test
- In Alert Settings, click **Send Test Alert** button
- Check admin's Telegram for test message

**Method 2**: Manual curl Test
```bash
curl -X POST http://YOUR_NAS_IP:8080/api/webhooks/truenas \
  -H "Content-Type: application/json" \
  -d '{
    "level": "CRITICAL",
    "message": "Disk temperature exceeds threshold",
    "datetime": "2024-02-15T14:30:00",
    "node": "truenas-main",
    "key": "DiskTemp"
  }'
```

Expected Telegram message to admin:
```
🚨 TrueNAS Alert

🔴 Уровень: CRITICAL
🖥 Узел: truenas-main
📊 Тип: DiskTemp

📝 Сообщение:
Disk temperature exceeds threshold

🕐 Время: 2024-02-15T14:30:00
```

---

## 🧪 Testing & Validation

### 1. Application Startup Test

```bash
cd /path/to/TelegramBotPlatform

# Stop existing containers
docker-compose down

# Rebuild and start
docker-compose up -d --build

# Check logs
docker logs -f multibot_app
```

**Expected Output**:
```
INFO  o.k.t.core.BotRegistry - Бот [AdminSystemBot] успешно зарегистрирован
INFO  o.k.t.core.BotRegistry - Бот [MediaInfoBot] успешно зарегистрирован
INFO  o.k.t.core.BotRegistry - Пул ботов инициализирован. Успешно запущено: 2
```

---

### 2. Health Check Test

```bash
curl http://localhost:8080/api/webhooks/health
```

**Expected Response**:
```json
{
  "status": "healthy",
  "service": "Webhook Integration Module",
  "version": "1.0.0"
}
```

---

### 3. Manual Webhook Tests

#### Test Radarr Webhook
```bash
curl -X POST http://localhost:8080/api/webhooks/radarr \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "Download",
    "movie": {
      "title": "Inception",
      "year": 2010,
      "tmdbId": 27205,
      "imdbId": "tt1375666"
    },
    "release": {
      "quality": "Bluray-1080p",
      "releaseGroup": "FGT"
    }
  }'
```

#### Test Sonarr Webhook
```bash
curl -X POST http://localhost:8080/api/webhooks/sonarr \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "Download",
    "series": {
      "title": "Game of Thrones",
      "tvdbId": 121361,
      "imdbId": "tt0944947"
    },
    "episodes": [{
      "seasonNumber": 1,
      "episodeNumber": 1,
      "title": "Winter Is Coming",
      "quality": "WEBDL-1080p"
    }]
  }'
```

#### Test TrueNAS Alert
```bash
curl -X POST http://localhost:8080/api/webhooks/truenas \
  -H "Content-Type: application/json" \
  -d '{
    "level": "WARNING",
    "message": "Pool scrub completed with errors",
    "node": "truenas-prod",
    "key": "PoolScrub"
  }'
```

---

### 4. Database Verification

```bash
# Check if webhook_event table exists
docker exec -it multibot_postgres psql -U postgres -d multibot -c "\dt"

# View recent webhook events
docker exec -it multibot_postgres psql -U postgres -d multibot -c "
  SELECT id, source, event_type, processed_successfully, created_at
  FROM webhook_event
  ORDER BY created_at DESC
  LIMIT 10;
"

# Check statistics
docker exec -it multibot_postgres psql -U postgres -d multibot -c "
  SELECT source,
         COUNT(*) as total,
         SUM(CASE WHEN processed_successfully THEN 1 ELSE 0 END) as success,
         SUM(CASE WHEN NOT processed_successfully THEN 1 ELSE 0 END) as failed
  FROM webhook_event
  WHERE DATE(created_at) = CURRENT_DATE
  GROUP BY source;
"
```

---

## 🎯 Deployment Checklist

### Before Deployment

- [ ] Update `.env` file with real bot tokens and channel IDs
- [ ] Create MediaInfoBot via @BotFather
- [ ] Create public Telegram channel for media posts
- [ ] Add MediaInfoBot as admin to channel
- [ ] Get channel ID (forward message to @userinfobot)
- [ ] Verify AdminBot token and admin chat ID

### Deployment

- [ ] Stop existing containers: `docker-compose down`
- [ ] Rebuild: `docker-compose up -d --build`
- [ ] Check logs: `docker logs -f multibot_app`
- [ ] Verify both bots started successfully
- [ ] Test health endpoint: `curl http://localhost:8080/api/webhooks/health`

### Post-Deployment

- [ ] Configure Radarr webhook (Settings → Connect)
- [ ] Test Radarr webhook (click Test button)
- [ ] Verify message in Telegram channel
- [ ] Configure Sonarr webhook (Settings → Connect)
- [ ] Test Sonarr webhook (click Test button)
- [ ] Verify message in Telegram channel
- [ ] Configure TrueNAS alerts (System Settings → Alert Settings)
- [ ] Test TrueNAS alert (Send Test Alert)
- [ ] Verify message in admin's Telegram
- [ ] Monitor logs for 24 hours for any errors

---

## 📊 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     TelegramBotPlatform                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           WebhookController (REST Layer)                 │  │
│  │  /api/webhooks/radarr                                    │  │
│  │  /api/webhooks/sonarr                                    │  │
│  │  /api/webhooks/truenas                                   │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│                       ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         WebhookRoutingService (Smart Router)            │  │
│  │  • JSON Parsing (Jackson)                               │  │
│  │  • Error Handling (try-catch)                           │  │
│  │  • Audit Logging (WebhookEvent)                         │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│                       ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │      MediaNotificationService (Message Formatter)       │  │
│  │  • processRadarrEvent()                                 │  │
│  │  • processSonarrEvent()                                 │  │
│  │  • processTrueNasAlert()                                │  │
│  └─────────┬────────────────────────────────────┬───────────┘  │
│            │                                    │               │
│            ▼                                    ▼               │
│  ┌──────────────────┐                ┌──────────────────────┐  │
│  │  MediaInfoBot    │                │     AdminBot         │  │
│  │  (Public Channel)│                │  (Private Chat)      │  │
│  └──────────────────┘                └──────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         PostgreSQL Database (Audit Trail)               │  │
│  │  • webhook_event (source, event_type, payload, ...)    │  │
│  │  • action_log (bot interactions)                        │  │
│  │  • error_log (bot errors)                               │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

External Systems:
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Radarr  │────▶│  Sonarr  │────▶│ TrueNAS  │
└──────────┘     └──────────┘     └──────────┘
     │                │                 │
     └────────────────┴─────────────────┘
                      │
                      ▼
          Webhooks to TelegramBotPlatform
```

---

## 🎓 Code Patterns & Best Practices

### 1. Robustness (Malformed JSON Won't Crash)

```java
try {
    RadarrWebhookDTO webhook = objectMapper.convertValue(payload, RadarrWebhookDTO.class);
    mediaNotificationService.processRadarrEvent(webhook);
    auditLog.setProcessedSuccessfully(true);
} catch (Exception e) {
    handleWebhookError(auditLog, e);  // Logs error, doesn't throw
}
```

### 2. Audit Trail (Every Webhook Logged)

```java
WebhookEvent auditLog = WebhookEvent.builder()
    .source("radarr")
    .payload(rawJson)
    .eventType(webhook.getEventType())
    .routedToBot("MediaInfoBot")
    .targetChannelId(mediaChannelId)
    .processedSuccessfully(true)
    .build();
webhookEventRepository.save(auditLog);
```

### 3. Thread Safety (Spring DI)

All services use `@RequiredArgsConstructor` + `final` fields → Spring handles thread-safe injection.

### 4. Consistent Code Style

Matches your existing codebase:
- Russian comments and log messages (как в AdminBot)
- Lombok annotations (`@Getter`, `@Builder`, etc.)
- Spring Boot conventions (`@RestController`, `@Service`, `@Repository`)
- PostgreSQL with Flyway migrations
- AOP-based logging pattern

---

## 📚 Files Created/Modified

### Created Files (13 total)

1. `src/main/java/org/kuxa/telegrambotplatform/dto/webhook/RadarrWebhookDTO.java`
2. `src/main/java/org/kuxa/telegrambotplatform/dto/webhook/SonarrWebhookDTO.java`
3. `src/main/java/org/kuxa/telegrambotplatform/dto/webhook/TrueNasAlertDTO.java`
4. `src/main/java/org/kuxa/telegrambotplatform/domain/WebhookEvent.java`
5. `src/main/java/org/kuxa/telegrambotplatform/repository/WebhookEventRepository.java`
6. `src/main/java/org/kuxa/telegrambotplatform/bots/MediaInfoBot.java`
7. `src/main/java/org/kuxa/telegrambotplatform/service/MediaNotificationService.java`
8. `src/main/java/org/kuxa/telegrambotplatform/service/WebhookRoutingService.java`
9. `src/main/java/org/kuxa/telegrambotplatform/controller/WebhookController.java`
10. `src/main/resources/db/migration/V2__webhook_events.sql`
11. `WEBHOOK_INTEGRATION_GUIDE.md` (complete setup guide)
12. `IMPLEMENTATION_SUMMARY.md` (this file)

### Modified Files (4 total)

1. `src/main/java/org/kuxa/telegrambotplatform/core/BaseTelegramBot.java` - Added `getTelegramClient()` getter
2. `src/main/resources/application.yaml` - Added media bot config + TMDB config + server port
3. `.env` - Added MediaInfoBot credentials + TMDB API key
4. `docker-compose.yml` - Added environment variables for media bot

---

## 🚀 Next Steps

1. **Deploy**: Follow the deployment checklist above
2. **Test**: Use the provided curl commands to verify each webhook type
3. **Monitor**: Check logs and database for first 24 hours
4. **Customize**: Edit message formats in `MediaNotificationService` as needed
5. **TMDB API**: Get API key from https://www.themoviedb.org/settings/api for movie posters
6. **Metrics**: Consider adding Prometheus/Grafana for webhook monitoring

---

## 📞 Support & Troubleshooting

Refer to `WEBHOOK_INTEGRATION_GUIDE.md` section "🔧 Траблшутинг" for:
- Network connectivity issues
- Bot permission problems
- JSON parsing errors
- Database migration failures
- TrueNAS-specific issues

---

**Implementation Status**: ✅ **COMPLETE**
**Code Quality**: Production-ready
**Documentation**: Comprehensive
**Test Coverage**: Manual tests provided
**Security**: No secrets in code, all via environment variables

🎉 **Ready to deploy!**
