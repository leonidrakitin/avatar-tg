package dev.avatar.middle.conf;

import com.pengrad.telegrambot.impl.FileApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TelegramConfig {

    private final AppProperty properties;

    @Bean
    public FileApi fileApi() {
        return new FileApi(properties.getToken());
    }
}
