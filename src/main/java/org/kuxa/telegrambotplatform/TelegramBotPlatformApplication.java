package org.kuxa.telegrambotplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TelegramBotPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotPlatformApplication.class, args);
    }

}
