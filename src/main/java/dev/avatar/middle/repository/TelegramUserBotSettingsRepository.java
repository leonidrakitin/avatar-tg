package dev.avatar.middle.repository;

import dev.avatar.middle.entity.TelegramUserBotSettingsEntity;
import dev.avatar.middle.model.ResponseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelegramUserBotSettingsRepository extends JpaRepository<TelegramUserBotSettingsEntity, UUID> {

    Optional<TelegramUserBotSettingsEntity> findByBotTokenIdAndTelegramChatId(String botTokenId, Long telegramChatId);

    @Modifying
    @Transactional
    @Query("""
        update TelegramUserBotSettingsEntity settings
        set settings.responseType = :responseType
        where settings.telegramChatId = :chatId and settings.botTokenId = :botTokenId
    """)
    void updateResponseType(String botTokenId, Long chatId, ResponseType responseType);
}
