package org.kuxa.telegrambotplatform.config;

import org.kuxa.telegrambotplatform.core.BotRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfig {
    @Bean
    public CommandLineRunner initBotsRunner(BotRegistry registry) {
        return args -> registry.startAll();
    }
}