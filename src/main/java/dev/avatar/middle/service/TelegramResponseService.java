package dev.avatar.middle.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideoNote;
import com.pengrad.telegrambot.request.SendVoice;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.model.ChatTempData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Async
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramResponseService {

    private final ChatDataService chatDataService;

    public void processWaiting(String botToken, Long chatId) {
        ChatTempData chatTempData = chatDataService.get(botToken, chatId)
                .orElseThrow(() ->
                        new RuntimeException("Unexpected behavior, chat data not found for chat id " + chatId)
                ); //todo chatdataexceptions
        if (
                chatTempData.getCurrentMockMessageId() == null
                && chatTempData.getCreatedAt().plus(Duration.ofSeconds(30)).isBefore(LocalDateTime.now()) //todo to property (exactly duration) ->  duration: 30s or duration: 1m
        ) {
            this.sendMockMessage(chatTempData, "💻 Your request has been accepted! Please wait..."); //todo i18n
        }
    }

    public void sendMockMessage(ChatTempData chatTempData, String text) {
        if (chatTempData.getCurrentMockMessageId() != null) {
            chatTempData.getBot().execute(new DeleteMessage(chatTempData.getChatId(), chatTempData.getCurrentMockMessageId()));
        }
        SendResponse sendResponse = chatTempData.getBot().execute(new SendMessage(chatTempData.getChatId(), text));
        Optional.ofNullable(sendResponse.message())
                .map(Message::messageId)
                .ifPresent(chatTempData::setCurrentMockMessageId);
    }

    public void sendMessage(ChatTempData chatData, String content) {
        chatData.getBot().execute(
                new SendMessage(chatData.getChatId(), content)
                        .parseMode(ParseMode.Markdown)
                        .replyToMessageId(chatData.getCurrentUserMessageId())
        );
        this.chatDataService.clearMessageData(chatData);
    }

    public void sendPhoto(String botToken, Long chatId, byte[] photo, String caption) {
        this.chatDataService.get(botToken, chatId)
                .ifPresent(chatData -> {
                    TelegramBot bot = chatData.getBot();
                    this.deleteMockMessageIfExists(bot, chatData);
                    bot.execute(new SendPhoto(chatData.getChatId(), photo)
                            .caption(caption)
                            .replyToMessageId(chatData.getCurrentUserMessageId())
                            .replyMarkup(new InlineKeyboardMarkup())
                    );
                    this.chatDataService.clearMessageData(chatData);
                });
    }

    public void sendUploadStatus(String botToken, Long chatId) {
        this.chatDataService.get(botToken, chatId)
                .ifPresent(chatData ->
                        chatData.getBot().execute(new SendChatAction(chatData.getBot(), ChatAction.upload_photo))
                );
    }

    public void sendVideoNote(String botToken, Long chatId, byte[] videoBytes) {
        this.chatDataService.get(botToken, chatId)
                .ifPresent(chatData -> {
                    TelegramBot bot = chatData.getBot();
                    this.deleteMockMessageIfExists(bot, chatData);
                    Optional.ofNullable(chatData.getCaption())
                                    .ifPresent(caption -> bot.execute(new SendMessage(chatData.getChatId(), caption)));
                    bot.execute(
                            new SendVideoNote(chatData.getChatId(), videoBytes)
                                    .replyToMessageId(chatData.getCurrentUserMessageId()) // todo why it does not work?
                    );
                    this.chatDataService.clearMessageData(chatData);
                });
    }

    public void sendVoice(String botToken, Long chatId, byte[] audioBytes, String caption) {
        this.chatDataService.get(botToken, chatId)
                .ifPresent(chatData -> {
                    TelegramBot bot = chatData.getBot();
                    bot.execute(new SendChatAction(chatId, ChatAction.upload_voice));
                    this.deleteMockMessageIfExists(bot, chatData);
                    bot.execute(
                            new SendVoice(chatData.getChatId(), audioBytes)
                                    .caption(caption)
                                    .replyToMessageId(chatData.getCurrentUserMessageId())
                    );
                    this.chatDataService.clearMessageData(chatData);
                });
    }

    public void deleteMockMessageIfExists(TelegramBot bot, ChatTempData chatTempData) {
        if (chatTempData.getCurrentMockMessageId() != null) {
            bot.execute(new DeleteMessage(chatTempData.getChatId(), chatTempData.getCurrentMockMessageId()));
        }
    }
}
