package org.kuxa.telegrambotplatform.aop;

import org.kuxa.telegrambotplatform.core.BaseTelegramBot;
import org.kuxa.telegrambotplatform.domain.ErrorLog;
import org.kuxa.telegrambotplatform.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.io.PrintWriter;
import java.io.StringWriter;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionAspect {

    private final ErrorLogRepository errorLogRepository;

    @Value("${admin.bot.token}")
    private String adminBotToken;

    @Value("${admin.chat.id}")
    private Long adminChatId;

    @AfterThrowing(
            pointcut = "execution(public void org.kuxa.telegrambotplatform.core.BaseTelegramBot+.consume(..))",
            throwing = "ex"
    )
    public void handleAndAlertExceptions(JoinPoint joinPoint, Throwable ex) {
        BaseTelegramBot bot = (BaseTelegramBot) joinPoint.getTarget();
        String botName = bot.getBotName();

        // Экстракция трассировки стека в строку
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        log.error("Перехвачено исключение в боте [{}]: {}", botName, ex.getMessage());

        // 1. Персистенция инцидента в базу данных
        ErrorLog errorLog = ErrorLog.builder()
                .botName(botName)
                .errorMessage(ex.getMessage())
                .stackTrace(stackTrace)
                .build();
        errorLogRepository.save(errorLog);

        // 2. Синхронная отправка тревожного уведомления (Alert) администратору
        dispatchTelegramAlert(botName, ex.getMessage());
    }

    private void dispatchTelegramAlert(String failingBotName, String errorMessage) {
        try {
            var alertClient = new OkHttpTelegramClient(adminBotToken);

            String alertText = String.format(
                    "🚨 *КРИТИЧЕСКАЯ ОШИБКА ПЛАТФОРМЫ*\n\n" +
                            "🤖 *Бот:* `%s`\n" +
                            "❌ *Ошибка:* %s\n\n" +
                            "Подробности сохранены в таблице `error_log`.",
                    failingBotName, errorMessage
            );

            SendMessage message = SendMessage.builder()
                    .chatId(adminChatId)
                    .text(alertText)
                    .parseMode("Markdown")
                    .build();

            alertClient.execute(message);
        } catch (Exception telegramException) {
            log.error("Сбой системы алертинга. Невозможно доставить уведомление администратору", telegramException);
        }
    }
}