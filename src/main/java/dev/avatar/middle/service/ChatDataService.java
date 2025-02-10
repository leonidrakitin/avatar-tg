package dev.avatar.middle.service;

import dev.avatar.middle.model.ChatTempData;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatDataService {
    //todo entity + repository + audit
    private final ConcurrentHashMap<String, ChatTempData> queueChatIdWithChatData = new ConcurrentHashMap<>();

    public Optional<ChatTempData> get(String botToken, Long chatId) {
        return Optional.ofNullable(queueChatIdWithChatData.get(botToken + chatId));
    }

    public void save(ChatTempData chatTempData) {
        queueChatIdWithChatData.put(chatTempData.getBot().getToken() + chatTempData.getChatId(), chatTempData);
    }

    public boolean isWaitingForAnswer(String botToken, Long chatId) {
        return Optional.ofNullable(queueChatIdWithChatData.get(botToken + chatId))
                .filter(chatData -> chatData.getCurrentUserMessageId() != null)
                .isPresent();
    }

    public void clearCallbackData(ChatTempData chatTempData) {
        chatTempData.setCallbackType(null);
        chatTempData.setCurrentMockMessageId(null);
        chatTempData.setCurrentUserMessageId(null);
        chatTempData.setCaption(null);
    }


    public void clearMessageData(ChatTempData chatTempData) {
        chatTempData.setCurrentMockMessageId(null);
        chatTempData.setCurrentUserMessageId(null);
        chatTempData.setCaption(null);
    }
}
