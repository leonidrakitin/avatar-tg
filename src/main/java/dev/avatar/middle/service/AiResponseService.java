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
import dev.avatar.middle.model.ChatData;
import dev.avatar.middle.service.ai.ElevenLabsService;
import dev.avatar.middle.service.ai.HeyGenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiResponseService {

    private final ChatDataService chatDataService;
    private final TelegramResponseService telegramResponseService;
    private final HeyGenService heyGenService;
    private final ElevenLabsService elevenLabsService;

    public void sendMessage(Long chatId, String content) {
        ChatData chatData = this.chatDataService.getByChatId(chatId)
                .orElseThrow(() ->
                        new RuntimeException("Unexpected behavior, chat data not found for chat id  " + chatId)
                ); //todo chatdataexceptions

        TelegramBot bot = chatData.getBot();
        this.telegramResponseService.deleteMockMessageIfExists(bot, chatData);
        switch (chatData.getResponseType()) {
            case TEXT: {
                bot.execute(
                        new SendMessage(chatId, content)
                                .parseMode(ParseMode.Markdown)
                                .replyToMessageId(chatData.getCurrentUserMessageId())
                );
                break;
            }
            case VIDEO: {
                chatData.setCaption(content);
                this.heyGenService.sendGenerateVideoRequest(chatId, content);
                this.telegramResponseService.sendMockMessage(chatData, "⏳ Video is being generated..."); //todo i18n
                bot.execute(new SendChatAction(chatId, ChatAction.record_video_note));
                break;
            }
            case VOICE: {
                bot.execute(new SendChatAction(chatId, ChatAction.record_voice));
                this.elevenLabsService.generateAudioFromText(content)
                        .subscribe((byte[] audio) -> this.telegramResponseService.sendVoice(chatId, audio, content));
                break;
            }
        }
    }
}
