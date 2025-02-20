package dev.avatar.middle.service;

import dev.avatar.middle.model.ChatTempData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatDataService {
    //todo entity + repository + audit
    private final ConcurrentHashMap<String, ChatTempData> queueChatIdWithChatData = new ConcurrentHashMap<>();

    public Optional<ChatTempData> get(String botToken, Long chatId) {
        return Optional.ofNullable(queueChatIdWithChatData.get(botToken + chatId));
    }

    public ChatTempData save(ChatTempData chatTempData) {
        return queueChatIdWithChatData.put(chatTempData.getBot().getToken() + chatTempData.getChatId(), chatTempData);
    }

    public boolean isWaitingForAnswer(String botToken, Long chatId) {
        return Optional.ofNullable(queueChatIdWithChatData.get(botToken + chatId))
                .filter(chatData -> chatData.getCurrentUserMessageId() != null)
                .isPresent();
    }

    public void clearMessageData(ChatTempData chatTempData) {
        chatTempData.setCurrentMockMessageId(null);
        chatTempData.setCurrentUserMessageId(null);
        chatTempData.setCaption(null);
        log.info("Cleared data for bot {} chatId {}", chatTempData.getBot().getToken(), chatTempData.getChatId());
    }

    public void clearMessageData(String botToken, Long chatId) {
        this.get(botToken, chatId).ifPresent(this::clearMessageData);
    }
}
