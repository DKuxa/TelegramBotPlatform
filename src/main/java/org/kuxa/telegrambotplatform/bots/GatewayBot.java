package org.kuxa.telegrambotplatform.bots;

import lombok.Getter;
import org.kuxa.telegrambotplatform.config.RabbitConfiguration;
import org.kuxa.telegrambotplatform.dto.BotRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * The sole Telegram bot in the Gateway Service.
 * Receives every incoming Update and forwards it as a {@link BotRequest}
 * to the {@code telegram.updates} RabbitMQ queue — no business logic here.
 */
@Component
public class GatewayBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    @Getter
    private final TelegramClient telegramClient;

    private final String botToken;
    private final RabbitTemplate rabbitTemplate;

    public GatewayBot(
            @Value("${gateway.bot.token}") String botToken,
            RabbitTemplate rabbitTemplate) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        BotRequest request = new BotRequest(botToken, update);
        rabbitTemplate.convertAndSend(RabbitConfiguration.UPDATES_QUEUE, request);
    }
}
