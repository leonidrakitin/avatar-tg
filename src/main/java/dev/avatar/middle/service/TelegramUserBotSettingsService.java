package dev.avatar.middle.service;

import dev.avatar.middle.entity.TelegramUserBotSettingsEntity;
import dev.avatar.middle.model.Language;
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

    public TelegramUserBotSettingsEntity createIfNotExists(String botTokenId, Long chatId, String languageCode) {
        TelegramUserBotSettingsEntity userBotSettings =
                this.repository.findByBotTokenIdAndTelegramChatId(botTokenId, chatId)
                        .map(settings -> settings.toBuilder().languageCode(languageCode).build())
                        .orElseGet(() ->
                                TelegramUserBotSettingsEntity.builder()
                                        .id(UUID.randomUUID())
                                        .telegramChatId(chatId)
                                        .botTokenId(botTokenId)
                                        .responseType(ResponseType.TEXT)
                                        .languageCode(languageCode)
                                        .build()
                        );

        return this.repository.save(userBotSettings);
    }

    public TelegramUserBotSettingsEntity getOrCreateIfNotExists(String botTokenId, Long chatId, String languageCode) {
        return this.createIfNotExists(botTokenId, chatId, languageCode);
    }

    public TelegramUserBotSettingsEntity createIfNotExists(String botTokenId, Long chatId) {
        return this.createIfNotExists(botTokenId, chatId, Language.EN.toString().toLowerCase());
    }

    public void updateUserResponseType(String botTokenId, Long chatId, ResponseType responseType) {
        this.repository.updateResponseType(botTokenId, chatId, responseType);
    }
}
