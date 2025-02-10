package dev.avatar.middle.repository;

import dev.avatar.middle.entity.ElevenLabsData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ElevenLabsDataRepository extends JpaRepository<ElevenLabsData, UUID> {

    Optional<ElevenLabsData> findByBotTokenId(String botTokenId);
}
