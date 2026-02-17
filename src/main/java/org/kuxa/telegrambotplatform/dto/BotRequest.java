package org.kuxa.telegrambotplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Message published to the {@code telegram.updates} queue.
 * The downstream worker uses {@code botToken} to identify which bot received the update.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotRequest {

    private String botToken;
    private Update update;
}
