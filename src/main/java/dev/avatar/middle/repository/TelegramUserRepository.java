package dev.avatar.middle.repository;

import dev.avatar.middle.entity.TelegramUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelegramUserRepository extends JpaRepository<TelegramUserEntity, Long> {
    Optional<TelegramUserEntity> findByChatIdAndAccessToUpDaily(Long chatId, boolean accessToUpDaily);
    List<TelegramUserEntity> findByAccessToUpDaily(boolean accessToUpDaily);
}
