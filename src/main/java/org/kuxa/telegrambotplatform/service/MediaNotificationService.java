package org.kuxa.telegrambotplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kuxa.telegrambotplatform.bots.AdminBot;
import org.kuxa.telegrambotplatform.bots.MediaInfoBot;
import org.kuxa.telegrambotplatform.dto.webhook.RadarrWebhookDTO;
import org.kuxa.telegrambotplatform.dto.webhook.SonarrWebhookDTO;
import org.kuxa.telegrambotplatform.dto.webhook.TrueNasAlertDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for formatting and sending media content notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaNotificationService {

    private final MediaInfoBot mediaInfoBot;
    private final AdminBot adminBot;

    @Value("${tmdb.poster.base.url:https://image.tmdb.org/t/p/w500}")
    private String tmdbPosterBaseUrl;

    @Value("${admin.chat.id}")
    private Long adminChatId;

    /**
     * Processes event from Radarr and sends notification.
     */
    public void processRadarrEvent(RadarrWebhookDTO webhook) {
        if (webhook.isTestEvent()) {
            log.info("Received test webhook from Radarr. Ignoring.");
            return;
        }

        String message = formatRadarrMessage(webhook);

        // If TMDB ID exists, try to send with poster
        if (webhook.getMovie() != null && webhook.getMovie().getTmdbId() != null) {
            String posterUrl = buildTmdbPosterUrl(webhook.getMovie().getTmdbId());
            mediaInfoBot.publishMediaWithPoster(posterUrl, message);
        } else {
            mediaInfoBot.publishMediaNotification(message);
        }

        log.info("Radarr notification processed successfully: {}", webhook.getMovieTitle());
    }

    /**
     * Processes event from Sonarr and sends notification.
     */
    public void processSonarrEvent(SonarrWebhookDTO webhook) {
        if (webhook.isTestEvent()) {
            log.info("Received test webhook from Sonarr. Ignoring.");
            return;
        }

        String message = formatSonarrMessage(webhook);

        // TODO: For Sonarr, posters can be added via TVDB API
        mediaInfoBot.publishMediaNotification(message);

        log.info("Sonarr notification processed successfully: {}", webhook.getSeriesTitle());
    }

    /**
     * Processes alert from TrueNAS and sends to AdminBot.
     */
    public void processTrueNasAlert(TrueNasAlertDTO alert) {
        String message = formatTrueNasMessage(alert);

        // Send alert directly to admin via AdminBot's telegram client
        sendAlertToAdmin(message);

        log.warn("TrueNAS Alert processed: {} - {}", alert.getLevel(), alert.getAlertType());
    }

    /**
     * Formats message for Radarr.
     */
    private String formatRadarrMessage(RadarrWebhookDTO webhook) {
        StringBuilder sb = new StringBuilder();

        if (webhook.isGrabEvent()) {
            sb.append("🎬 *Фильм добавлен в загрузку*\n\n");
        } else if (webhook.isDownloadEvent()) {
            sb.append("✅ *Фильм загружен*\n\n");
        } else {
            sb.append("🎬 *Событие Radarr: ").append(webhook.getEventType()).append("*\n\n");
        }

        sb.append("📽 *Название:* ").append(webhook.getMovieTitle()).append("\n");
        sb.append("🎞 *Качество:* `").append(webhook.getQualityProfile()).append("`\n");
        sb.append("🏷 *Релиз-группа:* `").append(webhook.getReleaseGroupName()).append("`\n");

        if (webhook.getMovie() != null && webhook.getMovie().getImdbId() != null) {
            sb.append("🔗 [IMDb](https://www.imdb.com/title/")
              .append(webhook.getMovie().getImdbId()).append("/)\n");
        }

        if (Boolean.TRUE.equals(webhook.getIsUpgrade())) {
            sb.append("\n⬆️ *This is a quality upgrade*");
        }

        return sb.toString();
    }

    /**
     * Formats message for Sonarr.
     */
    private String formatSonarrMessage(SonarrWebhookDTO webhook) {
        StringBuilder sb = new StringBuilder();

        if (webhook.isGrabEvent()) {
            sb.append("📺 *Сериал добавлен в загрузку*\n\n");
        } else if (webhook.isDownloadEvent()) {
            sb.append("✅ *Эпизод загружен*\n\n");
        } else {
            sb.append("📺 *Событие Sonarr: ").append(webhook.getEventType()).append("*\n\n");
        }

        sb.append("🎬 *Сериал:* ").append(webhook.getSeriesTitle()).append("\n");
        sb.append("📺 *Эпизод:* ").append(webhook.getEpisodesDescription()).append("\n");
        sb.append("🎞 *Качество:* `").append(webhook.getQualityProfile()).append("`\n");
        sb.append("🏷 *Релиз-группа:* `").append(webhook.getReleaseGroupName()).append("`\n");

        if (webhook.getSeries() != null && webhook.getSeries().getImdbId() != null) {
            sb.append("🔗 [IMDb](https://www.imdb.com/title/")
              .append(webhook.getSeries().getImdbId()).append("/)\n");
        }

        if (Boolean.TRUE.equals(webhook.getIsUpgrade())) {
            sb.append("\n⬆️ *This is a quality upgrade*");
        }

        return sb.toString();
    }

    /**
     * Formats message for TrueNAS Alert.
     */
    private String formatTrueNasMessage(TrueNasAlertDTO alert) {
        StringBuilder sb = new StringBuilder();

        sb.append(alert.getSeverityEmoji()).append(" *TrueNAS Alert*\n\n");
        sb.append("🔴 *Уровень:* `").append(alert.getLevel()).append("`\n");
        sb.append("🖥 *Узел:* `").append(alert.getNodeName()).append("`\n");
        sb.append("📊 *Тип:* `").append(alert.getAlertType()).append("`\n\n");
        sb.append("📝 *Сообщение:*\n").append(alert.getAlertMessage()).append("\n");

        if (alert.getDatetime() != null) {
            sb.append("\n🕐 *Время:* ").append(alert.getDatetime());
        }

        return sb.toString();
    }

    /**
     * Builds poster URL from TMDB.
     */
    private String buildTmdbPosterUrl(Integer tmdbId) {
        // Note: For full functionality, TMDB API key is required
        // Simplified scheme is used here
        return String.format("https://image.tmdb.org/t/p/w500/%d.jpg", tmdbId);
    }

    /**
     * Sends alert to admin via direct Telegram API call.
     * Does not use consume() methodology, as this is an outgoing message.
     */
    private void sendAlertToAdmin(String alertText) {
        try {
            var message = org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
                    .chatId(adminChatId)
                    .text(alertText)
                    .parseMode("Markdown")
                    .build();

            // Use TelegramClient from AdminBot
            adminBot.getTelegramClient().execute(message);

            log.info("TrueNAS Alert successfully delivered to admin");

        } catch (Exception e) {
            log.error("Critical error sending TrueNAS Alert to admin", e);
        }
    }

    /**
     * Returns target channel ID for Webhook audit.
     */
    public Long getMediaChannelId() {
        return mediaInfoBot.getMediaChannelId();
    }
}
