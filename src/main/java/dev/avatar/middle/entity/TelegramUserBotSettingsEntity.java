package dev.avatar.middle.entity;

import dev.avatar.middle.model.ResponseType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "telegram_user_settings")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TelegramUserBotSettingsEntity {

    @Id
    private final UUID id;

    private final Long telegramChatId;

    private final String botTokenId;

    @Enumerated(EnumType.STRING)
    private final ResponseType responseType;
}
