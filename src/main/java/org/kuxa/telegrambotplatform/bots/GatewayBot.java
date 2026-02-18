package org.kuxa.telegrambotplatform.bots;

import lombok.extern.slf4j.Slf4j;
import org.kuxa.telegrambotplatform.config.GatewayProperties;
import org.kuxa.telegrambotplatform.config.RabbitConfiguration;
import org.kuxa.telegrambotplatform.dto.BotRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * The sole Telegram bot in the Gateway Service.
 * Receives every incoming Update and forwards it as a {@link BotRequest}
 * to the {@code telegram.updates} RabbitMQ queue — no business logic here.
 */
@Slf4j
@Component
public class GatewayBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final GatewayProperties properties;
    private final RabbitTemplate rabbitTemplate;

    public GatewayBot(GatewayProperties properties, RabbitTemplate rabbitTemplate) {
        this.properties = properties;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public String getBotToken() {
        return properties.token();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        log.debug("Received update id={} from Telegram", update.getUpdateId());
        BotRequest request = new BotRequest(properties.token(), update);
        rabbitTemplate.convertAndSend(RabbitConfiguration.UPDATES_QUEUE, request);
    }
}
