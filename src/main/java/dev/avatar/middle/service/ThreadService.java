package dev.avatar.middle.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.avatar.middle.client.OpenAiAssistantClient;
import dev.avatar.middle.client.dto.Data;
import dev.avatar.middle.entity.ThreadEntity;
import dev.avatar.middle.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadService {

    private final OpenAiAssistantClient openAiAssistantClient;
    private final ThreadRepository threadRepository;

    private final Cache<Long, Data.Thread> threadCache = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();

    public Data.Thread getThreadByTelegramChatId(Long telegramChatId) throws ExecutionException {
        return this.threadCache.get(telegramChatId, () -> this.loadThread(telegramChatId));
    }

    private Data.Thread loadThread(Long telegramChatId) {
        return this.threadRepository.findByTelegramChatId(telegramChatId)
                .map(ThreadEntity::getThreadId)
                .map(this.openAiAssistantClient::retrieveThread)
                .orElseGet(() -> this.createThread(telegramChatId));
    }

    private Data.Thread createThread(Long telegramChatId) {
        Data.Thread thread = this.openAiAssistantClient.createThread(new Data.ThreadRequest());
        this.threadRepository.save(ThreadEntity.of(thread.id(), telegramChatId));
        return thread;
    }
}
