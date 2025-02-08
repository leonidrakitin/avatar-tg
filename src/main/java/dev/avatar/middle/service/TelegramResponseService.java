package dev.avatar.middle.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendAudio;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideo;
import com.pengrad.telegrambot.request.SendVideoNote;
import com.pengrad.telegrambot.request.SendVoice;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.model.ChatData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramResponseService {

    private final ChatDataService chatDataService;

    public void processWaiting(Long chatId) {
        ChatData chatData = chatDataService.getByChatId(chatId)
                .orElseThrow(() ->
                        new RuntimeException("Unexpected behavior, chat data not found for chat id " + chatId)
                ); //todo chatdataexceptions
        if (
                chatData.getCurrentMockMessageId() == null
                && chatData.getCreatedAt().plus(Duration.ofSeconds(30)).isBefore(LocalDateTime.now()) //todo to property (exactly duration) ->  duration: 30s or duration: 1m
        ) {
            this.sendMockMessage(chatData, "💻 Your request has been accepted! Please wait..."); //todo i18n
        }
    }

    public void sendMockMessage(ChatData chatData, String text) {
        if (chatData.getCurrentMockMessageId() != null) {
            chatData.getBot().execute(new DeleteMessage(chatData.getChatId(), chatData.getCurrentMockMessageId()));
        }
        SendResponse sendResponse = chatData.getBot().execute(new SendMessage(chatData.getChatId(), text));
        Optional.ofNullable(sendResponse.message())
                .map(Message::messageId)
                .ifPresent(chatData::setCurrentMockMessageId);
    }

    public void sendPhoto(Long telegramChatId, byte[] photo, String caption) {
        this.chatDataService.getByChatId(telegramChatId)
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

    public void sendUploadStatus(Long telegramChatId) {
        this.chatDataService.getByChatId(telegramChatId)
                .ifPresent(chatData ->
                        chatData.getBot().execute(new SendChatAction(chatData.getBot(), ChatAction.upload_photo))
                );
    }

    public void sendVideoNote(Long chatId, byte[] videoBytes) {
        this.chatDataService.getByChatId(chatId)
                .ifPresent(chatData -> {
                    TelegramBot bot = chatData.getBot();
                    bot.execute(new SendChatAction(chatData.getChatId(), ChatAction.upload_video_note));
                    this.deleteMockMessageIfExists(bot, chatData);
                    bot.execute(new SendMessage(chatData.getChatId(), chatData.getCaption()));
                    bot.execute(
                            new SendVideoNote(chatData.getChatId(), videoBytes)
                                    .replyToMessageId(chatData.getCurrentUserMessageId()) // todo why it does not work?
                    );
                    this.chatDataService.clearMessageData(chatData);
                });
    }

    public void sendVoice(Long chatId, byte[] audioBytes, String caption) {
        this.chatDataService.getByChatId(chatId)
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

    public void deleteMockMessageIfExists(TelegramBot bot, ChatData chatData) {
        if (chatData.getCurrentMockMessageId() != null) {
            bot.execute(new DeleteMessage(chatData.getChatId(), chatData.getCurrentMockMessageId()));
        }
    }
}
