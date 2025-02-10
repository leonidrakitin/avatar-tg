package dev.avatar.middle.service;

import dev.avatar.middle.entity.TelegramUserBotSettingsEntity;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.repository.TelegramUserBotSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUserBotSettingsService {

    private final TelegramUserBotSettingsRepository repository;

    public TelegramUserBotSettingsEntity createIfNotExists(String botTokenId, Long chatId) {
        return this.repository.findByBotTokenIdAndTelegramChatId(botTokenId, chatId)
                .orElseGet(() -> this.repository.save(
                        TelegramUserBotSettingsEntity.builder()
                                .id(UUID.randomUUID())
                                .telegramChatId(chatId)
                                .botTokenId(botTokenId)
                                .responseType(ResponseType.TEXT)
                                .build()
                ));
    }

    public void updateUserResponseType(String botTokenId, Long chatId, ResponseType responseType) {
        this.repository.updateResponseType(botTokenId, chatId, responseType);
    }
}
