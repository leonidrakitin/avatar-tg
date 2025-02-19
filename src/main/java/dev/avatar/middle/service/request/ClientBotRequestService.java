package dev.avatar.middle.service.request;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.SendChatAction;
import dev.avatar.middle.entity.TelegramUserBotSettingsEntity;
import dev.avatar.middle.entity.TelegramUserEntity;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.ChatTempData;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.CallbackBotRepository;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.TelegramFileService;
import dev.avatar.middle.service.TelegramUserBotSettingsService;
import dev.avatar.middle.service.TelegramUserService;
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
    private final TelegramUserService telegramUserService;
    private final ChatDataService chatDataService;

    public ClientBotRequestService(
            ChatDataService chatDataService,
            List<TelegramCallbackProcessor> callbacks,
            AssistantService assistantService,
            TelegramUserService telegramUserService,
            TelegramFileService telegramFileService,
            TelegramUserBotSettingsService telegramUserBotSettingsService,
            CallbackBotRepository callbackBotRepository
    ) {
        super(chatDataService, callbackBotRepository, callbacks);
        this.assistantService = assistantService;
        this.telegramUserService = telegramUserService;
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
        TelegramUserEntity telegramUserEntity = this.telegramUserService.createIfNotExists(message.from());

        try {
            if (message.voice() != null) {
                this.processVoiceMessage(bot, messageId, message.voice().fileId(), telegramUserEntity, chatId);
            }
            else if (message.videoNote() != null) {
                this.processVoiceMessage(bot, messageId, message.videoNote().fileId(), telegramUserEntity, chatId);
            }
            else {
                this.sendRequest(bot, messageId, text, chatId);
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
            TelegramUserEntity telegramUser,
            long chatId
    ) {
        byte[] fileData = this.telegramFileService.getTelegramFile(bot.getExecutableBot(), fileId);
        TelegramUserBotSettingsEntity settings = this.telegramUserBotSettingsService.getOrCreateIfNotExists(
                bot.getToken(), chatId, telegramUser.getDefaultLocale()
        );
        String transcribedAudio = this.assistantService.transcriptAudio(fileData, settings.getLanguageCode());
        log.debug("Got result from transcription audio service: {}", transcribedAudio);
        this.sendRequest(bot, messageId, transcribedAudio, chatId);
    }

    private void sendRequest(
            Bot bot,
            int messageId,
            String text,
            long chatId
    ) {
        try {
            ResponseType responseType = this.telegramUserBotSettingsService.createIfNotExists(bot.getToken(), chatId)
                    .getResponseType();
            if (responseType == ResponseType.TEXT) {
                bot.getExecutableBot().execute(new SendChatAction(chatId, ChatAction.typing));
            }
            boolean success = this.assistantService.sendRequest(bot.getAssistantId(), chatId, bot.getToken(), text);
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
