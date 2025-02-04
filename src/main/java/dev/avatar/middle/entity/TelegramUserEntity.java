package dev.avatar.middle.entity;

import com.pengrad.telegrambot.model.User;
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

@Entity
@Table(name = "telegram_user")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TelegramUserEntity {

    @Id
    private final Long telegramUserId;

    private final String username;

    private final String firstName;

    private final String lastName;

    private final String defaultLocale;

    private final String selectedLocale;

    @Enumerated(EnumType.STRING)
    private final ResponseType responseType;

    public static TelegramUserEntity of(User user) {
        return TelegramUserEntity.builder()
                .telegramUserId(user.id())
                .username(user.username())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .defaultLocale(user.languageCode())
                .selectedLocale(user.languageCode())
                .build();
    }
}
