# 🔗 Webhook Integration Guide

Complete guide for setting up TelegramBotPlatform integration with Radarr, Sonarr, and TrueNAS Scale.

---

## 📋 Table of Contents

1. [System Architecture](#system-architecture)
2. [Application Setup](#application-setup)
3. [Radarr Integration](#radarr-integration)
4. [Sonarr Integration](#sonarr-integration)
5. [TrueNAS Scale Integration](#truenas-scale-integration)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)

---

## 🏗️ System Architecture

### Components

| Component | Purpose | Telegram Bot |
|-----------|---------|--------------|
| `WebhookController` | REST API endpoints for receiving webhooks | - |
| `WebhookRoutingService` | Event routing and logging | - |
| `MediaNotificationService` | Message formatting and sending | - |
| `MediaInfoBot` | Media content publishing | Public channel |
| `AdminBot` | System alerts | Private chat |

### Routing Diagram

```
┌─────────┐      ┌─────────┐      ┌─────────────────────┐
│ Radarr  │─────▶│         │      │                     │
└─────────┘      │         │      │  MediaInfoBot       │
                 │ Webhook │─────▶│  (Public Channel)   │
┌─────────┐      │ Router  │      │                     │
│ Sonarr  │─────▶│         │      └─────────────────────┘
└─────────┘      │         │
                 └────┬────┘      ┌─────────────────────┐
┌─────────┐          │           │                     │
│ TrueNAS │──────────┴──────────▶│  AdminBot           │
└─────────┘                      │  (Private Chat)     │
                                 │                     │
                                 └─────────────────────┘
```

---

## ⚙️ Application Setup

### 1. Creating MediaInfoBot in Telegram

1. Open [@BotFather](https://t.me/BotFather) in Telegram
2. Send command `/newbot`
3. Set name: `Media Info Bot`
4. Set username: `your_media_info_bot`
5. **Save the token** (format: `1234567890:ABCdefGHIjklMNOpqrsTUVwxyz`)

### 2. Creating Public Channel

1. Create a public channel in Telegram for media content publishing
2. Add MediaInfoBot as channel administrator (grant posting rights)
3. Get channel ID:
   - Send a message to the channel
   - Forward it to [@userinfobot](https://t.me/userinfobot)
   - Copy **Forwarded from chat** (format: `-1001234567890`)

### 3. Environment Variables Setup

Update `.env` file:

```env
# --- MediaInfoBot (for publishing media content) ---
MEDIA_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
MEDIA_BOT_NAME=MediaInfoBot
MEDIA_CHANNEL_ID=-1001234567890

# --- TMDB API (optional, for movie posters) ---
TMDB_API_KEY=your_tmdb_api_key_from_themoviedb.org
```

### 4. Application Restart

```bash
docker-compose down
docker-compose up -d --build
```

### 5. API Availability Check

```bash
curl http://localhost:8080/api/webhooks/health
```

Expected response:
```json
{
  "status": "healthy",
  "service": "Webhook Integration Module",
  "version": "1.0.0"
}
```

---

## 🎬 Radarr Integration

### Configuration Table

| Setting | Value |
|---------|-------|
| **URL** | `http://multibot_app:8080/api/webhooks/radarr` |
| **Method** | `POST` |
| **Username** | *(leave empty)* |
| **Password** | *(leave empty)* |

### Step-by-Step Instructions

#### Step 1: Open Radarr Settings

1. Go to Radarr Web UI (usually `http://your-nas-ip:7878`)
2. `Settings` → `Connect` → click `+` (Add Connection)

#### Step 2: Select Webhook

1. Select **Webhook** from the list
2. Fill in the form:

| Field | Value |
|-------|-------|
| Name | `TelegramBotPlatform` |
| On Grab | ✅ Enable |
| On Download | ✅ Enable |
| On Upgrade | ✅ Enable |
| On Rename | ⬜ Disable (optional) |
| On Movie Added | ⬜ Disable (optional) |
| On Movie Delete | ⬜ Disable |
| On Movie File Delete | ⬜ Disable |
| On Health Issue | ⬜ Disable |
| On Application Update | ⬜ Disable |
| Tags | *(leave empty for all movies)* |

#### Step 3: URL Configuration

**IMPORTANT**: URL depends on your Docker network:

**Option A**: All containers in the same Docker network:
```
http://multibot_app:8080/api/webhooks/radarr
```

**Option B**: Containers in different networks or Radarr not in Docker:
```
http://YOUR_NAS_IP:8080/api/webhooks/radarr
```

#### Step 4: Testing

1. Click **Test** button at the bottom of the form
2. Check application logs:
```bash
docker logs multibot_app | grep Radarr
```
3. A message should appear in Telegram channel: "🎬 Radarr Event: Test"

#### Step 5: Save

Click **Save**. Now you'll receive notifications in the channel for every movie download.

---

## 📺 Sonarr Integration

### Configuration Table

| Setting | Value |
|---------|-------|
| **URL** | `http://multibot_app:8080/api/webhooks/sonarr` |
| **Method** | `POST` |
| **Username** | *(leave empty)* |
| **Password** | *(leave empty)* |

### Step-by-Step Instructions

#### Step 1: Open Sonarr Settings

1. Go to Sonarr Web UI (usually `http://your-nas-ip:8989`)
2. `Settings` → `Connect` → click `+` (Add Connection)

#### Step 2: Select Webhook

1. Select **Webhook** from the list
2. Fill in the form:

| Field | Value |
|-------|-------|
| Name | `TelegramBotPlatform` |
| On Grab | ✅ Enable |
| On Import | ✅ Enable (= On Download) |
| On Upgrade | ✅ Enable |
| On Rename | ⬜ Disable (optional) |
| On Series Add | ⬜ Disable (optional) |
| On Series Delete | ⬜ Disable |
| On Episode File Delete | ⬜ Disable |
| On Health Issue | ⬜ Disable |
| On Application Update | ⬜ Disable |
| Tags | *(leave empty for all series)* |

#### Step 3: URL Configuration

**Option A**: All containers in the same Docker network:
```
http://multibot_app:8080/api/webhooks/sonarr
```

**Option B**: Containers in different networks or Sonarr not in Docker:
```
http://YOUR_NAS_IP:8080/api/webhooks/sonarr
```

#### Step 4: Testing

1. Click **Test** button at the bottom of the form
2. Check logs:
```bash
docker logs multibot_app | grep Sonarr
```
3. A message should appear in Telegram channel: "📺 Sonarr Event: Test"

#### Step 5: Save

Click **Save**.

---

## 🖥️ TrueNAS Scale Integration

### Configuration Table

| Setting | Value |
|---------|-------|
| **URL** | `http://multibot_app:8080/api/webhooks/truenas` |
| **Method** | `POST` |
| **Alert Levels** | CRITICAL, WARNING |

### Step-by-Step Instructions

#### Step 1: Open Alert Settings

1. Open TrueNAS Scale Web UI
2. Go to `System Settings` → `Alert Settings`
3. Click `Add` in **Alert Services** section

#### Step 2: Create Generic Webhook

1. Select type: **Generic Webhook** (or **Custom Webhook**)
2. Fill in the form:

| Field | Value |
|-------|-------|
| Name | `TelegramBotPlatform` |
| Enabled | ✅ Enable |
| Type | Generic Webhook |
| URL | `http://YOUR_NAS_IP:8080/api/webhooks/truenas` |
| Method | POST |
| Headers | `Content-Type: application/json` |

**IMPORTANT**: Use the TrueNAS host IP address, NOT `multibot_app`, because TrueNAS is not in the Docker network.

#### Step 3: Alert Filter Configuration

In **Alert Settings** section:

| Field | Value |
|-------|-------|
| Alert Level | `CRITICAL` and `WARNING` |
| Frequency | `IMMEDIATELY` |

#### Step 4: Testing

**Method 1**: Via TrueNAS UI
1. In alert settings click **Send Test Alert**
2. Check Telegram — admin should receive an alert

**Method 2**: Via curl
```bash
curl -X POST http://YOUR_NAS_IP:8080/api/webhooks/truenas \
  -H "Content-Type: application/json" \
  -d '{
    "level": "CRITICAL",
    "message": "Test alert from TrueNAS",
    "datetime": "2024-02-15T10:30:00",
    "node": "truenas-main",
    "key": "TestAlert"
  }'
```

#### Step 5: Verify in Telegram

Admin should receive a message:
```
🚨 TrueNAS Alert

🔴 Level: CRITICAL
🖥 Node: truenas-main
📊 Type: TestAlert

📝 Message:
Test alert from TrueNAS

🕐 Time: 2024-02-15T10:30:00
```

---

## 🧪 Testing

### 1. Health Check

```bash
curl http://localhost:8080/api/webhooks/health
```

### 2. Test Webhook

```bash
curl -X POST http://localhost:8080/api/webhooks/test \
  -H "Content-Type: application/json" \
  -d '{"test": "data", "source": "manual"}'
```

### 3. Simulate Radarr Webhook

```bash
curl -X POST http://localhost:8080/api/webhooks/radarr \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "Download",
    "movie": {
      "title": "The Matrix",
      "year": 1999,
      "tmdbId": 603,
      "imdbId": "tt0133093"
    },
    "release": {
      "quality": "Bluray-1080p",
      "releaseGroup": "SPARKS"
    }
  }'
```

### 4. Simulate Sonarr Webhook

```bash
curl -X POST http://localhost:8080/api/webhooks/sonarr \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "Download",
    "series": {
      "title": "Breaking Bad",
      "tvdbId": 81189,
      "imdbId": "tt0903747"
    },
    "episodes": [{
      "seasonNumber": 1,
      "episodeNumber": 1,
      "title": "Pilot",
      "quality": "WEBDL-1080p"
    }],
    "release": {
      "quality": "WEBDL-1080p",
      "releaseGroup": "NTb"
    }
  }'
```

### 5. Check Logs

```bash
# All logs
docker logs multibot_app

# Webhook events only
docker logs multibot_app | grep "webhook"

# Errors only
docker logs multibot_app | grep "ERROR"
```

### 6. Check Database

```bash
docker exec -it multibot_postgres psql -U postgres -d multibot -c "SELECT * FROM webhook_event ORDER BY created_at DESC LIMIT 5;"
```

---

## 🔧 Troubleshooting

### Problem: Webhook doesn't reach the application

**Symptoms**: Radarr/Sonarr shows connection error

**Solution**:
1. Check that the application is running:
```bash
docker ps | grep multibot_app
```

2. Check port 8080 availability:
```bash
curl http://localhost:8080/api/webhooks/health
```

3. Check Docker network:
```bash
docker network ls
docker network inspect <network_name>
```

4. If Radarr/Sonarr is in a different network, use host IP:
```
http://192.168.1.100:8080/api/webhooks/radarr
```

---

### Problem: Messages don't arrive in Telegram

**Symptoms**: Webhook is accepted (200 OK), but no messages

**Solution**:
1. Check bot logs:
```bash
docker logs multibot_app | grep "MediaInfoBot"
```

2. Verify MediaInfoBot is added as channel administrator

3. Make sure MEDIA_CHANNEL_ID starts with `-100`:
```bash
docker exec multibot_app env | grep MEDIA_CHANNEL_ID
```

4. Verify bot token:
```bash
curl https://api.telegram.org/bot<YOUR_TOKEN>/getMe
```

---

### Problem: TrueNAS alerts don't work

**Symptoms**: TrueNAS doesn't send webhooks

**Solution**:
1. TrueNAS must use host IP address, not `multibot_app`

2. Check firewall rules on TrueNAS:
```bash
# On TrueNAS host
curl http://<host-ip>:8080/api/webhooks/health
```

3. Check alert format in TrueNAS — it may differ from documented

4. Add logging to controller for debugging

---

### Problem: JSON parsing errors

**Symptoms**: Logs show `JsonProcessingException` or `UnrecognizedPropertyException`

**Solution**:
1. Check raw payload in `webhook_event` table:
```sql
SELECT payload FROM webhook_event WHERE processed_successfully = false ORDER BY created_at DESC LIMIT 1;
```

2. DTO classes use `@JsonIgnoreProperties(ignoreUnknown = true)`, but if structure differs significantly, DTO may need updating

3. Update Radarr/Sonarr version — webhook structure may have changed

---

### Problem: Database not updating

**Symptoms**: Flyway migrations don't apply

**Solution**:
1. Check schema version:
```bash
docker exec -it multibot_postgres psql -U postgres -d multibot -c "SELECT * FROM flyway_schema_history;"
```

2. If V2 migration didn't apply, apply manually:
```bash
docker exec -i multibot_postgres psql -U postgres -d multibot < src/main/resources/db/migration/V2__webhook_events.sql
```

3. Rebuild application:
```bash
docker-compose down
docker-compose up -d --build
```

---

## 📊 Monitoring and Analytics

### Check Webhook Statistics

```sql
-- Events by source for today
SELECT source, COUNT(*) as total,
       SUM(CASE WHEN processed_successfully THEN 1 ELSE 0 END) as successful,
       SUM(CASE WHEN NOT processed_successfully THEN 1 ELSE 0 END) as failed
FROM webhook_event
WHERE DATE(created_at) = CURRENT_DATE
GROUP BY source;

-- Last 10 errors
SELECT source, event_type, error_message, created_at
FROM webhook_event
WHERE processed_successfully = false
ORDER BY created_at DESC
LIMIT 10;

-- Top 5 most frequent events this week
SELECT source, event_type, COUNT(*) as count
FROM webhook_event
WHERE created_at >= NOW() - INTERVAL '7 days'
GROUP BY source, event_type
ORDER BY count DESC
LIMIT 5;
```

---

## 🎯 Next Steps

1. **TMDB API integration**: Configure TMDB API key for actual movie posters
2. **Message customization**: Edit `MediaNotificationService` to change notification format
3. **Additional filters**: Add filtering by tags/quality in Radarr/Sonarr
4. **Monitoring**: Set up Grafana + Prometheus for webhook metrics visualization

---

## 📚 Useful Links

- [Radarr Webhook Documentation](https://wiki.servarr.com/radarr/settings#connections)
- [Sonarr Webhook Documentation](https://wiki.servarr.com/sonarr/settings#connections)
- [TrueNAS Scale Alert Documentation](https://www.truenas.com/docs/scale/scaletutorials/systemsettings/advanced/settingupscalealerts/)
- [Telegram Bot API](https://core.telegram.org/bots/api)
- [TMDB API](https://developers.themoviedb.org/3)

---

**Author**: TelegramBotPlatform Integration Team
**Version**: 1.0.0
**Date**: 2024-02-15
