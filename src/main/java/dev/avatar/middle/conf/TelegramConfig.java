package dev.avatar.middle.conf;

import com.pengrad.telegrambot.impl.FileApi;
import dev.avatar.middle.entity.TelegramBotEntity;
import dev.avatar.middle.repository.TelegramBotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class TelegramConfig {

    private final TelegramBotRepository telegramBotRepository;

    @Bean
    public Map<String, FileApi> tgfileApiMap() {
        return telegramBotRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        TelegramBotEntity::getBotTokenId, bot -> new FileApi(bot.getBotTokenId())
                ));
    }
}
