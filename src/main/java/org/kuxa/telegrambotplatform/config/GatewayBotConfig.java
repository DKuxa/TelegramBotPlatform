package org.kuxa.telegrambotplatform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Configures the Telegram client as a managed Spring bean.
 * Separates bot-specific wiring from RabbitMQ infrastructure configuration.
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayBotConfig {

    @Bean
    public TelegramClient telegramClient(GatewayProperties properties) {
        return new OkHttpTelegramClient(properties.token());
    }
}
