package org.kuxa.telegrambotplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kuxa.telegrambotplatform.config.RabbitConfiguration;
import org.kuxa.telegrambotplatform.dto.BotResponse;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Consumes reply messages from the {@code telegram.replies} queue
 * and dispatches them back to users via the Telegram API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyListener {

    private final TelegramClient telegramClient;

    @RabbitListener(queues = RabbitConfiguration.REPLIES_QUEUE)
    public void onReply(BotResponse response) {
        SendMessage message = SendMessage.builder()
                .chatId(response.chatId())
                .text(response.text())
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send reply to chatId={}: {}", response.chatId(), e.getMessage(), e);
        }
    }
}
