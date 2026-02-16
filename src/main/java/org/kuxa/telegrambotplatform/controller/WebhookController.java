package org.kuxa.telegrambotplatform.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kuxa.telegrambotplatform.service.WebhookRoutingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for receiving Webhooks from external systems (Radarr, Sonarr, TrueNAS).
 *
 * Endpoints:
 * - POST /api/webhooks/radarr  → Webhook from Radarr (movies)
 * - POST /api/webhooks/sonarr  → Webhook from Sonarr (TV series)
 * - POST /api/webhooks/truenas → Webhook from TrueNAS (system alerts)
 *
 * All methods work asynchronously: immediately return 200 OK,
 * and processing is delegated to WebhookRoutingService.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookRoutingService webhookRoutingService;

    /**
     * Endpoint for Webhook from Radarr.
     * Example URL for Radarr configuration: http://multibot_app:8080/api/webhooks/radarr
     */
    @PostMapping("/radarr")
    public ResponseEntity<Map<String, String>> receiveRadarrWebhook(@RequestBody Map<String, Object> payload) {
        log.info("📥 Received Radarr webhook. Payload size: {} fields", payload.size());

        try {
            webhookRoutingService.handleRadarrWebhook(payload);
            return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "message", "Radarr webhook accepted for processing"
            ));

        } catch (Exception e) {
            log.error("Critical error processing Radarr webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to process Radarr webhook: " + e.getMessage()
            ));
        }
    }

    /**
     * Endpoint for Webhook from Sonarr.
     * Example URL for Sonarr configuration: http://multibot_app:8080/api/webhooks/sonarr
     */
    @PostMapping("/sonarr")
    public ResponseEntity<Map<String, String>> receiveSonarrWebhook(@RequestBody Map<String, Object> payload) {
        log.info("📥 Received Sonarr webhook. Payload size: {} fields", payload.size());

        try {
            webhookRoutingService.handleSonarrWebhook(payload);
            return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "message", "Sonarr webhook accepted for processing"
            ));

        } catch (Exception e) {
            log.error("Critical error processing Sonarr webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to process Sonarr webhook: " + e.getMessage()
            ));
        }
    }

    /**
     * Endpoint for Alert from TrueNAS Scale.
     * Example URL for TrueNAS configuration: http://multibot_app:8080/api/webhooks/truenas
     */
    @PostMapping("/truenas")
    public ResponseEntity<Map<String, String>> receiveTrueNasAlert(@RequestBody Map<String, Object> payload) {
        log.info("📥 Received TrueNAS alert. Payload size: {} fields", payload.size());

        try {
            webhookRoutingService.handleTrueNasWebhook(payload);
            return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "message", "TrueNAS alert accepted for processing"
            ));

        } catch (Exception e) {
            log.error("Critical error processing TrueNAS alert", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to process TrueNAS alert: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint for verifying Webhook API availability.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "Webhook Integration Module",
            "version", "1.0.0"
        ));
    }

    /**
     * Test endpoint for verifying routing functionality.
     * Can be used for debugging without configuring external systems.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testWebhook(@RequestBody Map<String, Object> payload) {
        log.info("🧪 Received test webhook. Payload: {}", payload);

        return ResponseEntity.ok(Map.of(
            "status", "test_success",
            "received_fields", String.valueOf(payload.size()),
            "message", "Test webhook successfully received"
        ));
    }
}
