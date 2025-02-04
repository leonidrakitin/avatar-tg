package dev.avatar.middle.repository;

import dev.avatar.middle.entity.ThreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ThreadRepository extends JpaRepository<ThreadEntity, String> {

    @Query("""
        select t from ThreadEntity t
        where t.telegramChatId = :telegramChatId and t.deprecatedAt is null
    """)
    Optional<ThreadEntity> findByTelegramChatId(Long telegramChatId);
}
