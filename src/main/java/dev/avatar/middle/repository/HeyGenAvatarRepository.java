package dev.avatar.middle.repository;

import dev.avatar.middle.entity.HeyGenAvatar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HeyGenAvatarRepository extends JpaRepository<HeyGenAvatar, UUID> {

    Optional<HeyGenAvatar> findByBotTokenId(String botTokenId);
}
