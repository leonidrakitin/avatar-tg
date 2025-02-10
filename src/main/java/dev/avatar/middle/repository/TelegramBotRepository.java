package dev.avatar.middle.repository;

import dev.avatar.middle.entity.TelegramBotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramBotRepository extends JpaRepository<TelegramBotEntity, Long> {
    Optional<TelegramBotEntity> findByBotTokenId(String botTokenId);
}
