package dev.avatar.middle.repository;

import dev.avatar.middle.entity.TelegramUserEntity;
import dev.avatar.middle.model.ResponseType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface TelegramUserRepository extends JpaRepository<TelegramUserEntity, Long> {

    @Modifying
    @Transactional
    @Query("""
        update TelegramUserEntity user
        set user.responseType = :responseType
        where user.telegramUserId = :telegramUserId
    """)
    void updateResponseType(long telegramUserId, ResponseType responseType);

}
