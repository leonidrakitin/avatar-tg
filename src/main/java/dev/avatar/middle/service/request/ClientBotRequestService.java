package dev.avatar.middle.service.request;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.SendChatAction;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.ChatTempData;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.TelegramFileService;
import dev.avatar.middle.service.TelegramUserBotSettingsService;
import dev.avatar.middle.service.ai.AssistantService;
import dev.avatar.middle.service.telegram.callback.TelegramCallbackProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class ClientBotRequestService extends AbstractBotRequestService {

    private final AssistantService assistantService;
    private final TelegramFileService telegramFileService;
    private final TelegramUserBotSettingsService telegramUserBotSettingsService;
    private final ChatDataService chatDataService;

    public ClientBotRequestService(
            ChatDataService chatDataService,
            List<TelegramCallbackProcessor> callbacks,
            AssistantService assistantService,
            TelegramFileService telegramFileService,
            TelegramUserBotSettingsService telegramUserBotSettingsService
    ) {
        super(chatDataService, callbacks);
        this.assistantService = assistantService;
        this.telegramFileService = telegramFileService;
        this.telegramUserBotSettingsService = telegramUserBotSettingsService;
        this.chatDataService = chatDataService;
    }

    @Override
    public void handleMessageUpdate(Bot bot, Message message) {
        long chatId = message.chat().id();
        long telegramUserId = message.from().id();
        int messageId = message.messageId();
        String text = message.text();

        try {
            if (message.voice() != null) {
                this.processVoiceMessage(bot, messageId, message.voice().fileId(), message.from(), chatId);
            }
            else if (message.videoNote() != null) {
                this.processVoiceMessage(bot, messageId, message.videoNote().fileId(), message.from(), chatId);
            }
            else {
                this.sendRequest(bot, messageId, text, message.from(), chatId);
            }
        }
        catch (Exception e) {
            log.error("Error processing message for user {}: {}", telegramUserId, e.getMessage(), e); //todo i18n
//            bot.execute(new SendMessage(chatId, "❌ An error occurred while processing your request."));
        }
    }

    @Override
    public TelegramBotType getSupportedBotType() {
        return TelegramBotType.CLIENT_BOT;
    }

    private void processVoiceMessage(
            Bot bot,
            int messageId,
            String fileId,
            User telegramUser,
            long chatId
    ) {
        byte[] fileData = this.telegramFileService.getTelegramFile(bot.getExecutableBot(), fileId);
        String transcribedAudio = this.assistantService.transcriptAudio(fileData, telegramUser.languageCode());
        log.debug("Got result from transcription audio service: {}", transcribedAudio); //todo i18n
        this.sendRequest(bot, messageId, transcribedAudio, telegramUser, chatId);
    }

    private void sendRequest(
            Bot bot,
            int messageId,
            String text,
            User telegramUser,
            long chatId
    ) {
        try {
            ResponseType responseType = this.telegramUserBotSettingsService.createIfNotExists(bot.getToken(), chatId)
                    .getResponseType();
            if (responseType == ResponseType.TEXT) {
                bot.getExecutableBot().execute(new SendChatAction(chatId, ChatAction.typing));
            }
            boolean success = this.assistantService.sendRequest(
                    bot.getAssistantId(), telegramUser.id(), bot.getToken(), text
            );
            if (success) {
                this.chatDataService.save(new ChatTempData(chatId, messageId, bot.getExecutableBot(), responseType));
            }
        }
        catch (ExecutionException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e); //todo add here more cases and log.errors etc
        }
    }
}
