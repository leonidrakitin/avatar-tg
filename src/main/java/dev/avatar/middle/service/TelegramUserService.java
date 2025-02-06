package dev.avatar.middle.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.Data;
import com.pengrad.telegrambot.model.User;
import dev.avatar.middle.entity.TelegramUserEntity;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.repository.AssistantRepository;
import dev.avatar.middle.repository.TelegramUserRepository;
import dev.avatar.middle.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUserService {

    private final ThreadRepository threadRepository;
    private final TelegramUserRepository telegramUserRepository;
    private final AssistantRepository assistantRepository;
    private final AssistantApi assistantApi;

    private final Cache<Long, Data.Thread> threadCache = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();

    public TelegramUserEntity createIfNotExists(User telegramUser) {
        return this.telegramUserRepository.findById(telegramUser.id())
                .orElseGet(() ->
                        this.telegramUserRepository.save(
                                TelegramUserEntity.builder()
                                        .telegramUserId(telegramUser.id())
                                        .responseType(ResponseType.TEXT)
                                        .defaultLocale(telegramUser.languageCode())
                                        .selectedLocale(telegramUser.languageCode())
                                        .firstName(telegramUser.firstName())
                                        .lastName(telegramUser.lastName())
                                        .username(telegramUser.username())
                                        .build()
                        )
                );
    }

    public void updateUserResponseType(Long telegramUserId, ResponseType responseType) {
        this.telegramUserRepository.updateResponseType(telegramUserId, responseType);
    }

    // todo getOrCreate
}
