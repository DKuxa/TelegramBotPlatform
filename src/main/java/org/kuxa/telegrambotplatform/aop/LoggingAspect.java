package org.kuxa.telegrambotplatform.aop;

import org.kuxa.telegrambotplatform.core.BaseTelegramBot;
import org.kuxa.telegrambotplatform.domain.ActionLog;
import org.kuxa.telegrambotplatform.repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.CompletableFuture;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {

    private final ActionLogRepository actionLogRepository;

    @Around("execution(public void org.kuxa.telegrambotplatform.core.BaseTelegramBot+.consume(org.telegram.telegrambots.meta.api.objects.Update)) && args(update)")
    public Object logUpdateToDatabase(ProceedingJoinPoint joinPoint, Update update) throws Throwable {

        BaseTelegramBot bot = (BaseTelegramBot) joinPoint.getTarget();
        String botName = bot.getBotName();

        CompletableFuture.runAsync(() -> persistLog(botName, update))
                .exceptionally(ex -> {
                    log.error("Критический сбой асинхронного логирования для бота {}", botName, ex);
                    return null;
                });

        return joinPoint.proceed();
    }

    private void persistLog(String botName, Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            ActionLog actionLog = ActionLog.builder()
                    .botName(botName)
                    .chatId(update.getMessage().getChatId())
                    .messageText(update.getMessage().getText())
                    .build();

            actionLogRepository.save(actionLog);
        }
    }
}