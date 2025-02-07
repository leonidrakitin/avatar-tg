package dev.avatar.middle.service;

import dev.avatar.middle.model.ChatData;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatDataService {
    //todo entity + repository + audit
    private final ConcurrentHashMap<Long, ChatData> queueChatIdWithChatData = new ConcurrentHashMap<>();

    public Optional<ChatData> getByChatId(Long chatId) {
        return Optional.ofNullable(queueChatIdWithChatData.get(chatId));
    }

    public void save(ChatData chatData) {
        queueChatIdWithChatData.put(chatData.getChatId(), chatData);
    }

    public boolean isWaitingForAnswer(Long chatId) {
        return Optional.ofNullable(queueChatIdWithChatData.get(chatId))
                .filter(chatData -> chatData.getCurrentUserMessageId() != null)
                .isPresent();
    }

    public void clearCallbackData(ChatData chatData) {
        chatData.setCallbackType(null);
        chatData.setCurrentMockMessageId(null);
        chatData.setCurrentUserMessageId(null);
    }

    public void clearMessageData(ChatData chatData) {
        chatData.setCurrentMockMessageId(null);
        chatData.setCurrentUserMessageId(null);
    }
}
