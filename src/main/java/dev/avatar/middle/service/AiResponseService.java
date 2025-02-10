package dev.avatar.middle.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.SendChatAction;
import dev.avatar.middle.model.ChatTempData;
import dev.avatar.middle.service.ai.ElevenLabsService;
import dev.avatar.middle.service.ai.HeyGenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiResponseService {

    private final ChatDataService chatDataService;
    private final TelegramResponseService telegramResponseService;
    private final HeyGenService heyGenService;
    private final ElevenLabsService elevenLabsService;

    public void sendMessage(String botToken, Long chatId, String content) {
        ChatTempData chatTempData = this.chatDataService.get(botToken, chatId)
                .orElseThrow(() ->
                        new RuntimeException("Unexpected behavior, chat data not found for chat id  " + chatId)
                ); //todo chatdataexceptions

        TelegramBot bot = chatTempData.getBot();
        this.telegramResponseService.deleteMockMessageIfExists(bot, chatTempData);
        switch (chatTempData.getResponseType()) {
            case TEXT: {
                this.telegramResponseService.sendMessage(chatTempData, content);
                break;
            }
            case VIDEO: {
                chatTempData.setCaption(content);
                this.heyGenService.sendGenerateVideoRequest(botToken, chatId, content);
                this.telegramResponseService.sendMockMessage(chatTempData, "⏳ Video is being generated..."); //todo i18n
                bot.execute(new SendChatAction(chatId, ChatAction.record_video_note));
                break;
            }
            case VOICE: {
                bot.execute(new SendChatAction(chatId, ChatAction.record_voice));
                this.elevenLabsService.generateAudioFromText(content)
                        .subscribe((byte[] audio) -> this.telegramResponseService.sendVoice(botToken, chatId, audio, content));
                break;
            }
        }
    }
}
