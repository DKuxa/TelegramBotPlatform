package org.kuxa.telegrambotplatform.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for auditing incoming Webhook events.
 * Stores information about all received webhooks for debugging and analytics.
 */
@Entity
@Table(name = "webhook_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source", nullable = false, length = 50)
    private String source; // "radarr", "sonarr", "truenas"

    @Column(name = "event_type", length = 100)
    private String eventType; // "Grab", "Download", "CRITICAL", etc.

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // Raw JSON for debugging

    @Column(name = "processed_successfully", nullable = false)
    private Boolean processedSuccessfully;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "routed_to_bot", length = 100)
    private String routedToBot; // Name of the bot that handled the request

    @Column(name = "target_channel_id")
    private Long targetChannelId; // Channel/chat ID where the message was sent

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
