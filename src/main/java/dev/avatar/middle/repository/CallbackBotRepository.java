package dev.avatar.middle.repository;

import dev.avatar.middle.entity.CallbackBotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CallbackBotRepository extends JpaRepository<CallbackBotEntity, UUID> {

    Optional<CallbackBotEntity> findByCallbackMessageIdAndAndBotTokenId(Long callbackMessageId, String botTokenId);
}
