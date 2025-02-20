package dev.avatar.middle.repository;

import dev.avatar.middle.entity.TelegramBotEntity;
import dev.avatar.middle.entity.TelegramUserEntity;
import dev.avatar.middle.model.TelegramBotType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelegramBotRepository extends JpaRepository<TelegramBotEntity, Long> {
    Optional<TelegramBotEntity> findByBotTokenId(String botTokenId);
    List<TelegramBotEntity> findByAdminAndBotType(TelegramUserEntity admin, TelegramBotType botType);
}
