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
import dev.avatar.middle.service.ai.HeyGenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramResponseService {

    private final ChatDataService chatDataService;
    private final VideoService videoService;
    private final VoiceService voiceService;

    public void processWaiting(Long telegramChatId) {
        ChatData chatData = chatDataService.getByChatId(telegramChatId);
        if (chatData == null) {
            log.error("Unexpected behavior, chat data not found for chat id {}", telegramChatId);
            return;
        }
        if (chatData.getCurrentMockMessageId() == null) {
            this.sendMockMessage(chatData, "💻 Your request has been accepted! Please wait..."); //todo i18n
        }
    }

    private void sendMockMessage(ChatData chatData, String text) {
        SendResponse sendResponse = chatData.getBot().execute(new SendMessage(chatData.getChatId(), text));
        Optional.ofNullable(sendResponse.message())
                .map(Message::messageId)
                .ifPresent(chatData::setCurrentMockMessageId);
    }

    public void sendMessage(Long telegramChatId, String content) {
        ChatData chatData = this.chatDataService.getByChatId(telegramChatId);
        if (chatData == null) {
            return;
        }
        TelegramBot bot = chatData.getBot();
        try {
            this.deleteMockMessageIfExists(bot, chatData);
            switch (chatData.getResponseType()) {
                case TEXT: {
                    bot.execute(
                            new SendMessage(chatData.getChatId(), content)
                                    .parseMode(ParseMode.Markdown)
                                    .replyToMessageId(chatData.getCurrentUserMessageId())
                    );
                    break;
                }
                case VIDEO: {
                    this.videoService.sendGenerateVideoRequest(chatData.getChatId(), content);
                    sendMockMessage(chatData, "⏳ Video is being generated..."); //todo i18n
                    break;
                }
                case VOICE: {
                    byte[] voiceResponse = this.voiceService.sendGenerateVideoRequest(chatData.getChatId(), content);
                    this.sendVoice(chatData.getChatId(), voiceResponse);
                }
            }
        }
        finally {
            chatDataService.clearMessageData(chatData);
        }
    }

    public void sendPhoto(Long telegramChatId, byte[] photo, String caption) {
        Optional.ofNullable(this.chatDataService.getByChatId(telegramChatId))
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
        Optional.ofNullable(this.chatDataService.getByChatId(telegramChatId))
                .ifPresent(chatData ->
                        chatData.getBot().execute(new SendChatAction(chatData.getBot(), ChatAction.upload_photo))
                );
    }

    public void sendVideoNote(Long telegramChatId, byte[] videoBytes, String caption) {
        Optional.ofNullable(this.chatDataService.getByChatId(telegramChatId))
                .ifPresent(chatData -> {
                    TelegramBot bot = chatData.getBot();
                    this.deleteMockMessageIfExists(bot, chatData);
                    bot.execute(
                            new SendVideoNote(chatData.getChatId(), videoBytes)
//                                    .replyToMessageId(chatData.getCurrentUserMessageId()) // todo why it does not work?
                    );
                });
    }

    public void sendVoice(Long telegramChatId, byte[] audioBytes){
        Optional.ofNullable(this.chatDataService.getByChatId(telegramChatId))
                .ifPresent(chatData -> {
                    TelegramBot bot = chatData.getBot();
                    this.deleteMockMessageIfExists(bot, chatData);
                    bot.execute(
                            new SendVoice(chatData.getChatId(), audioBytes)
                                    .replyToMessageId(chatData.getCurrentUserMessageId())
                    );
                });
    }

    private void deleteMockMessageIfExists(TelegramBot bot, ChatData chatData) {
        if (chatData.getCurrentMockMessageId() != null) {
            bot.execute(new DeleteMessage(chatData.getChatId(), chatData.getCurrentMockMessageId()));
        }
    }
}
