package org.kuxa.telegrambotplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kuxa.telegrambotplatform.domain.WebhookEvent;
import org.kuxa.telegrambotplatform.dto.webhook.RadarrWebhookDTO;
import org.kuxa.telegrambotplatform.dto.webhook.SonarrWebhookDTO;
import org.kuxa.telegrambotplatform.dto.webhook.TrueNasAlertDTO;
import org.kuxa.telegrambotplatform.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Router service for processing incoming Webhooks from various sources.
 * Identifies source, parses JSON, logs event, and routes to appropriate bot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookRoutingService {

    private final MediaNotificationService mediaNotificationService;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Processes Webhook from Radarr (movies).
     */
    @Transactional
    public void handleRadarrWebhook(Map<String, Object> rawPayload) {
        String rawJson = serializePayload(rawPayload);
        WebhookEvent auditLog = initWebhookEvent("radarr", rawJson);

        try {
            // Parse JSON into strongly-typed DTO
            RadarrWebhookDTO webhook = objectMapper.convertValue(rawPayload, RadarrWebhookDTO.class);

            auditLog.setEventType(webhook.getEventType());
            auditLog.setRoutedToBot("MediaInfoBot");
            auditLog.setTargetChannelId(mediaNotificationService.getMediaChannelId());

            // Delegate processing to MediaNotificationService
            mediaNotificationService.processRadarrEvent(webhook);

            auditLog.setProcessedSuccessfully(true);
            log.info("✅ Radarr webhook processed successfully: {}", webhook.getEventType());

        } catch (Exception e) {
            handleWebhookError(auditLog, e);
        } finally {
            webhookEventRepository.save(auditLog);
        }
    }

    /**
     * Processes Webhook from Sonarr (TV series).
     */
    @Transactional
    public void handleSonarrWebhook(Map<String, Object> rawPayload) {
        String rawJson = serializePayload(rawPayload);
        WebhookEvent auditLog = initWebhookEvent("sonarr", rawJson);

        try {
            SonarrWebhookDTO webhook = objectMapper.convertValue(rawPayload, SonarrWebhookDTO.class);

            auditLog.setEventType(webhook.getEventType());
            auditLog.setRoutedToBot("MediaInfoBot");
            auditLog.setTargetChannelId(mediaNotificationService.getMediaChannelId());

            mediaNotificationService.processSonarrEvent(webhook);

            auditLog.setProcessedSuccessfully(true);
            log.info("✅ Sonarr webhook processed successfully: {}", webhook.getEventType());

        } catch (Exception e) {
            handleWebhookError(auditLog, e);
        } finally {
            webhookEventRepository.save(auditLog);
        }
    }

    /**
     * Processes Alert from TrueNAS (system notifications).
     */
    @Transactional
    public void handleTrueNasWebhook(Map<String, Object> rawPayload) {
        String rawJson = serializePayload(rawPayload);
        WebhookEvent auditLog = initWebhookEvent("truenas", rawJson);

        try {
            TrueNasAlertDTO alert = objectMapper.convertValue(rawPayload, TrueNasAlertDTO.class);

            auditLog.setEventType(alert.getLevel());
            auditLog.setRoutedToBot("AdminBot");

            mediaNotificationService.processTrueNasAlert(alert);

            auditLog.setProcessedSuccessfully(true);
            log.info("✅ TrueNAS alert processed successfully: {} - {}", alert.getLevel(), alert.getAlertType());

        } catch (Exception e) {
            handleWebhookError(auditLog, e);
        } finally {
            webhookEventRepository.save(auditLog);
        }
    }

    /**
     * Initializes webhook audit record.
     */
    private WebhookEvent initWebhookEvent(String source, String payload) {
        return WebhookEvent.builder()
                .source(source)
                .payload(payload)
                .processedSuccessfully(false)
                .build();
    }

    /**
     * Handles webhook parsing/processing error.
     */
    private void handleWebhookError(WebhookEvent auditLog, Exception e) {
        String errorMsg = String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage());
        auditLog.setProcessedSuccessfully(false);
        auditLog.setErrorMessage(errorMsg);

        log.error("❌ Error processing webhook from {}: {}", auditLog.getSource(), errorMsg, e);
    }

    /**
     * Serializes payload to JSON for database storage.
     */
    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize payload to JSON: {}", e.getMessage());
            return payload.toString();
        }
    }
}
