package dev.avatar.middle.repository;

import dev.avatar.middle.entity.HeyGenData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HeygenDataRepository extends JpaRepository<HeyGenData, UUID> {

    Optional<HeyGenData> findByBotTokenId(String botTokenId);
}
