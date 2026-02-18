package org.kuxa.telegrambotplatform.dto;

/**
 * Message consumed from the {@code telegram.replies} queue.
 * The gateway uses {@code chatId} and {@code text} to send a Telegram message.
 */
public record BotResponse(String botToken, Long chatId, String text) {}
