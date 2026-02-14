package org.kuxa.telegrambotplatform.core;

import lombok.Getter;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public abstract class BaseTelegramBot implements LongPollingSingleThreadUpdateConsumer {

    @Getter
    private final String botToken;

    @Getter
    private final String botName;

    protected final TelegramClient telegramClient;

    public BaseTelegramBot(String botToken, String botName) {
        this.botToken = botToken;
        this.botName = botName;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public abstract void consume(Update update);
}