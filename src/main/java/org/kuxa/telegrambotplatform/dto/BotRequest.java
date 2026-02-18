package org.kuxa.telegrambotplatform.dto;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Message published to the {@code telegram.updates} queue.
 * The downstream worker uses {@code botToken} to identify which bot received the update.
 */
public record BotRequest(String botToken, Update update) {}
