package dev.avatar.middle.entity;

import dev.avatar.middle.model.TelegramBotType;
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

@Entity
@Table(name = "telegram_bot")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TelegramBotEntity {

    @Id
    private final Long telegramId;

    private final String name;

    @Enumerated(EnumType.STRING)
    private final TelegramBotType botType;
}
