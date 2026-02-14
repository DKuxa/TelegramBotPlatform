package org.kuxa.telegrambotplatform.bots;

import org.kuxa.telegrambotplatform.core.BaseTelegramBot;
import org.kuxa.telegrambotplatform.core.BotRegistry;
import org.kuxa.telegrambotplatform.domain.ErrorLog;
import org.kuxa.telegrambotplatform.repository.ActionLogRepository;
import org.kuxa.telegrambotplatform.repository.ErrorLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.format.DateTimeFormatter;
import java.util.List;


@Component
public class AdminBot extends BaseTelegramBot {

    private final BotRegistry botRegistry;
    private final ActionLogRepository actionLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final Long adminChatId;

    public AdminBot(
            @Value("${admin.bot.token}") String botToken,
            @Value("${admin.bot.name:AdminSystemBot}") String botName,
            @Value("${admin.chat.id}") Long adminChatId,
            BotRegistry botRegistry,
            ActionLogRepository actionLogRepository,
            ErrorLogRepository errorLogRepository) {
        super(botToken, botName);
        this.adminChatId = adminChatId;
        this.botRegistry = botRegistry;
        this.actionLogRepository = actionLogRepository;
        this.errorLogRepository = errorLogRepository;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() ||!update.getMessage().hasText()) return;

        Long chatId = update.getMessage().getChatId();

        if (!chatId.equals(adminChatId)) return;

        String command = update.getMessage().getText().trim();
        String responseText = processAdminCommand(command);

        dispatchResponse(chatId, responseText);
    }

    private String processAdminCommand(String command) {
        if (command.equalsIgnoreCase("/status")) {
            StringBuilder sb = new StringBuilder("📊 *Состояние кластера ботов:*\n\n");
            for (String name : botRegistry.getAllBotNames()) {
                sb.append("• `").append(name).append("` : ")
                        .append(botRegistry.getBotStatus(name)).append("\n");
            }
            return sb.toString();
        }
        else if (command.startsWith("/restart")) {
            List<String> parts = List.of(command.split("\\s+"));

            if (parts.size() < 2) return "⚠️ Использование: `/restart <bot_name>`";

            String targetBot = parts.get(1);
            boolean success = botRegistry.restartBot(targetBot);
            return success? "✅ Сессия бота `" + targetBot + "` успешно пересоздана."
                    : "❌ Ошибка: Бот `" + targetBot + "` не найден в реестре.";
        }
        else if (command.equalsIgnoreCase("/stats")) {
            long uniqueUsers = actionLogRepository.countUniqueUsersToday();
            return "📈 *Метрики за 24 часа:*\n\nУникальных пользователей (DAU): *" + uniqueUsers + "*";
        }
        else if (command.equalsIgnoreCase("/errors")) {
            return generateErrorReport();
        }

        return """
               🛠 *Панель управления платформой*
               `/status` — Мониторинг сессий (Health Check)
               `/restart <имя>` — Перезагрузка long-polling
               `/stats` — Метрики DAU
               `/errors` — Анализ инцидентов""";
    }

    private String generateErrorReport() {
        List<ErrorLog> latestErrors = errorLogRepository.findTop5ByOrderByCreatedAtDesc();
        if (latestErrors.isEmpty()) {
            return "✅ Системных инцидентов не зафиксировано.";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        StringBuilder sb = new StringBuilder("⚠️ *Последние 5 инцидентов:*\n\n");

        for (ErrorLog error : latestErrors) {
            sb.append("🕒 ").append(error.getCreatedAt().format(formatter)).append("\n")
                    .append("🤖 Бот: `").append(error.getBotName()).append("`\n")
                    .append("❌ ").append(error.getErrorMessage()).append("\n\n");
        }
        return sb.toString();
    }

    private void dispatchResponse(Long chatId, String text) {
        try {
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("Markdown")
                    .build();
            telegramClient.execute(msg);
        } catch (Exception e) {
            System.err.println("Сбой доставки ответа AdminBot: " + e.getMessage());
        }
    }
}