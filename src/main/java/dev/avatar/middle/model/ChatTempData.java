package dev.avatar.middle.model;

import com.pengrad.telegrambot.TelegramBot;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatTempData {
    private Long chatId;
    private TelegramBot bot; //todo bot Id, use botcache (guava) to get bots from there
    private ResponseType responseType;
    private Integer currentUserMessageId;
    private Integer currentMockMessageId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String caption;
    private String action;

    public ChatTempData(long chatId, int currentUserMessageId, TelegramBot bot, ResponseType responseType) {
        this.chatId = chatId;
        this.currentUserMessageId = currentUserMessageId;
        this.bot = bot;
        this.responseType = responseType;
    }

    public ChatTempData(long chatId, TelegramBot bot, String action) {
        this.chatId = chatId;
        this.bot = bot;
    }
}
