package org.kuxa.telegrambotplatform.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for processing Alerts from TrueNAS Scale.
 * TrueNAS sends alerts via Generic Webhook.
 *
 * Structure may vary depending on TrueNAS version.
 * Documentation: https://www.truenas.com/docs/scale/scaletutorials/systemsettings/advanced/settingupscalealerts/
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrueNasAlertDTO {

    @JsonProperty("level")
    private String level; // "CRITICAL", "WARNING", "INFO"

    @JsonProperty("message")
    private String message;

    @JsonProperty("datetime")
    private String datetime;

    @JsonProperty("node")
    private String node;

    @JsonProperty("id")
    private String id;

    @JsonProperty("text")
    private String text; // Альтернативное поле для сообщения

    @JsonProperty("key")
    private String key; // Alert type (e.g., "DiskTemp", "PoolStatus")

    @JsonProperty("klass")
    private String klass; // Alert class

    @JsonProperty("dismissed")
    private Boolean dismissed;

    // Helper methods
    public boolean isCritical() {
        return "CRITICAL".equalsIgnoreCase(level) || "ALERT".equalsIgnoreCase(level);
    }

    public boolean isWarning() {
        return "WARNING".equalsIgnoreCase(level) || "WARN".equalsIgnoreCase(level);
    }

    public boolean isInfo() {
        return "INFO".equalsIgnoreCase(level) || "NOTICE".equalsIgnoreCase(level);
    }

    public String getAlertMessage() {
        // TrueNAS may use different fields for message text
        if (message != null && !message.isEmpty()) {
            return message;
        }
        if (text != null && !text.isEmpty()) {
            return text;
        }
        return "TrueNAS Alert (no message body)";
    }

    public String getSeverityEmoji() {
        if (isCritical()) {
            return "🚨";
        } else if (isWarning()) {
            return "⚠️";
        } else {
            return "ℹ️";
        }
    }

    public String getNodeName() {
        return node != null ? node : "Unknown Node";
    }

    public String getAlertType() {
        return key != null ? key : "General Alert";
    }
}
