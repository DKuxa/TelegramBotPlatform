package org.kuxa.telegrambotplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.bot")
public record GatewayProperties(String token) {}
