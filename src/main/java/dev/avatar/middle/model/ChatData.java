package dev.avatar.middle.model;

import com.pengrad.telegrambot.TelegramBot;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatData {
    private Long chatId;
    private TelegramBot bot; //todo bot Id, use botcache (guava) to get bots from there
    private ResponseType responseType;
    private CallbackType callbackType;
    private Integer currentUserMessageId;
    private Integer currentMockMessageId;
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatData(
            long chatId,
            int currentUserMessageId,
            TelegramBot bot,
            Integer currentMockMessageId,
            ResponseType responseType
    ) {
        this.chatId = chatId;
        this.currentUserMessageId = currentUserMessageId;
        this.bot = bot;
        this.currentMockMessageId = currentMockMessageId;
        this.responseType = responseType;
    }

    public ChatData(long chatId, int currentUserMessageId, TelegramBot bot, ResponseType responseType) {
        this.chatId = chatId;
        this.currentUserMessageId = currentUserMessageId;
        this.bot = bot;
        this.responseType = responseType;
    }

    public ChatData(long chatId, TelegramBot bot) {
        this.chatId = chatId;
        this.bot = bot;
    }
}
