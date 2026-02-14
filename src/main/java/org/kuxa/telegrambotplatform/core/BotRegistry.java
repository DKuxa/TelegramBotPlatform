package org.kuxa.telegrambotplatform.core;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
@RequiredArgsConstructor
public class BotRegistry {

    private final List<BaseTelegramBot> availableBots;

    private TelegramBotsLongPollingApplication application;

    private final Map<String, BotSession> activeSessions = new ConcurrentHashMap<>();

    public void startAll() {
        try {
            application = new TelegramBotsLongPollingApplication();
            for (BaseTelegramBot bot : availableBots) {
                registerAndStart(bot);
            }
            log.info("Пул ботов инициализирован. Успешно запущено: {}", activeSessions.size());
        } catch (Exception e) {
            log.error("Критическая ошибка запуска пула ботов", e);
        }
    }


    private void registerAndStart(BaseTelegramBot bot) {
        try {
            BotSession session = application.registerBot(bot.getBotToken(), bot);
            activeSessions.put(bot.getBotName(), session);
            log.info("Бот [{}] успешно зарегистрирован в Telegram API.", bot.getBotName());
        } catch (Exception e) {
            log.error("Сбой регистрации бота [{}]", bot.getBotName(), e);
        }
    }


    public boolean restartBot(String botName) {
        BotSession session = activeSessions.get(botName);
        if (session!= null) {
            log.warn("Инициирована принудительная остановка сессии бота [{}]...", botName);
            session.stop(); // Разрыв long-polling соединения
            activeSessions.remove(botName);
        }

        return availableBots.stream()
                .filter(b -> b.getBotName().equalsIgnoreCase(botName))
                .findFirst()
                .map(bot -> {
                    registerAndStart(bot);
                    return true;
                })
                .orElse(false);
    }


    public String getBotStatus(String botName) {
        BotSession session = activeSessions.get(botName);
        if (session == null) return "DOWN (Not Registered)";
        return session.isRunning()? "UP (Polling Active)" : "DOWN (Session Stopped)";
    }

    public List<String> getAllBotNames() {
        return availableBots.stream().map(BaseTelegramBot::getBotName).toList();
    }

    @PreDestroy
    public void shutdown() {
        if (application!= null) {
            try {
                log.info("Остановка всех ботов перед закрытием контекста Spring...");
                application.stop(); // Штатное закрытие пула потоков
            } catch (Exception e) {
                log.error("Ошибка при штатном завершении работы Telegram Application", e);
            }
        }
    }
}