package dev.avatar.middle.service;

import com.pengrad.telegrambot.model.User;
import dev.avatar.middle.entity.TelegramUserEntity;
import dev.avatar.middle.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUserService {

    private final TelegramUserRepository telegramUserRepository;

    public TelegramUserEntity createIfNotExists(User telegramUser) {
        return this.telegramUserRepository.findById(telegramUser.id())
                .orElseGet(() ->
                        this.telegramUserRepository.save(
                                TelegramUserEntity.builder()
                                        .telegramUserId(telegramUser.id())
                                        .defaultLocale(telegramUser.languageCode())
                                        .selectedLocale(telegramUser.languageCode())
                                        .firstName(telegramUser.firstName())
                                        .lastName(telegramUser.lastName())
                                        .username(telegramUser.username())
                                        .build()
                        )
                );
    }

    // todo getOrCreate
}
