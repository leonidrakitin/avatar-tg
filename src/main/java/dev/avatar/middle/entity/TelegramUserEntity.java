package dev.avatar.middle.entity;

import com.pengrad.telegrambot.model.User;
import jakarta.persistence.Entity;
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

    private final Long chatId;

    private final String username;

    private final String firstName;

    private final String lastName;

    private final String defaultLocale;

    private final String selectedLocale;

    public static TelegramUserEntity of(Long chatId, User user) {
        return TelegramUserEntity.builder()
                .chatId(chatId)
                .telegramUserId(user.id())
                .username(user.username())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .defaultLocale(user.languageCode())
                .selectedLocale(user.languageCode())
                .build();
    }
}
