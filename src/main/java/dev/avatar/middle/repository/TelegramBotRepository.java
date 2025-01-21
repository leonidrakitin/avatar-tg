package dev.avatar.middle.repository;

import dev.avatar.middle.entity.TelegramBotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramBotRepository extends JpaRepository<TelegramBotEntity, Long> {
}
