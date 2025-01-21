package dev.avatar.middle.repository;

import dev.avatar.middle.entity.AssistantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssistantRepository extends JpaRepository<AssistantEntity, String> {
    Optional<AssistantEntity> findByTelegramBotId(String telegramBotId);
}
