package org.kuxa.telegrambotplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message consumed from the {@code telegram.replies} queue.
 * The gateway uses {@code chatId} and {@code text} to send a Telegram message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotResponse {

    private String botToken;
    private Long   chatId;
    private String text;
}
