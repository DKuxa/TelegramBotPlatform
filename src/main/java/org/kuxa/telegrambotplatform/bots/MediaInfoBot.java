package org.kuxa.telegrambotplatform.bots;

import lombok.extern.slf4j.Slf4j;
import org.kuxa.telegrambotplatform.core.BaseTelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Bot for publishing media content information to a public channel.
 * Used by Webhooks from Radarr/Sonarr for notifications about new movies/TV shows.
 */
@Slf4j
@Component
public class MediaInfoBot extends BaseTelegramBot {

    private final Long mediaChannelId;

    public MediaInfoBot(
            @Value("${media.bot.token}") String botToken,
            @Value("${media.bot.name:MediaInfoBot}") String botName,
            @Value("${media.channel.id}") Long mediaChannelId) {
        super(botToken, botName);
        this.mediaChannelId = mediaChannelId;
    }

    @Override
    public void consume(Update update) {
        // MediaInfoBot does not process user messages.
        // It is used only for outgoing notifications from Webhooks.
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        // Simple echo response for testing (optional)
        if ("/start".equalsIgnoreCase(text)) {
            sendTextMessage(chatId, "🎬 MediaInfoBot is active. This bot publishes media content notifications.");
        }
    }

    /**
     * Publishes a text message about media to the channel.
     */
    public void publishMediaNotification(String messageText) {
        sendTextMessage(mediaChannelId, messageText);
    }

    /**
     * Publishes a message with a poster (image) to the channel.
     * @param imageUrl Poster URL (e.g., from TMDB)
     * @param caption Image caption
     */
    public void publishMediaWithPoster(String imageUrl, String caption) {
        try {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(mediaChannelId)
                    .photo(new InputFile(imageUrl))
                    .caption(caption)
                    .parseMode("Markdown")
                    .build();

            telegramClient.execute(photo);
            log.info("Poster successfully published to channel {}", mediaChannelId);

        } catch (TelegramApiException e) {
            log.error("Failed to send poster to channel {}: {}", mediaChannelId, e.getMessage());
            // Fallback: send text message
            publishMediaNotification(caption + "\n\n[Poster unavailable]");
        }
    }

    /**
     * Helper method for sending text messages.
     */
    private void sendTextMessage(Long chatId, String text) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("Markdown")
                    .build();

            telegramClient.execute(message);
            log.info("Message successfully sent to chat {}", chatId);

        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Returns the target channel ID (for logging in WebhookEvent).
     */
    public Long getMediaChannelId() {
        return mediaChannelId;
    }
}
