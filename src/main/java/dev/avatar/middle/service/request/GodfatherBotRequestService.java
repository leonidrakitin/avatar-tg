package dev.avatar.middle.service.request;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.ChatTempData;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.TelegramFileService;
import dev.avatar.middle.service.TelegramUserService;
import dev.avatar.middle.service.ai.AssistantService;
import dev.avatar.middle.service.telegram.callback.TelegramCallbackProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class GodfatherBotRequestService extends AbstractBotRequestService {

    private final AssistantService assistantService;
    private final TelegramFileService telegramFileService;
    private final TelegramUserService telegramUserService;
    private final ChatDataService chatDataService;

    public GodfatherBotRequestService(
            ChatDataService chatDataService,
            List<TelegramCallbackProcessor> callbacks,
            AssistantService assistantService,
            TelegramFileService telegramFileService,
            TelegramUserService telegramUserService
    ) {
        super(chatDataService, callbacks);
        this.assistantService = assistantService;
        this.telegramFileService = telegramFileService;
        this.telegramUserService = telegramUserService;
        this.chatDataService = chatDataService;
    }

    @Override
    public void handleMessageUpdate(Bot bot, Message message) {
        long chatId = message.chat().id();
        long telegramUserId = message.from().id();
        int messageId = message.messageId();

        if (this.chatDataService.isWaitingForAnswer("", chatId)) {
            bot.getExecutableBot().execute(new SendMessage(chatId, "⌛️ Please ask me later when I finish processing your previous message!")); //todo i18n
            return;
        }
        try {
            if (message.voice() != null) {
                processVoiceMessage(bot, messageId, message.voice().fileId(), message.from(), chatId);
            }
            else if (message.videoNote() != null) {
                processVoiceMessage(bot, messageId, message.videoNote().fileId(), message.from(), chatId);
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
        String transcribedAudio = this.assistantService.transcriptAudio(fileData, "en");
        log.debug("Got result from transcription audio service: {}", transcribedAudio); //todo i18n
        this.sendRequest(bot, messageId, transcribedAudio, telegramUser, chatId);
    }

    private void processDocument(Bot bot, long telegramUserId, String fileId, String content) throws ExecutionException {
        byte[] fileData = this.telegramFileService.getTelegramFile(bot.getExecutableBot(), fileId);
        this.assistantService.processDocument(bot.getAssistantId(), telegramUserId, fileData, content);
    }

    private void sendRequest(
            Bot bot,
            int messageId,
            String text,
            long chatId
    ) {
        try {
            bot.getExecutableBot().execute(new SendChatAction(chatId, ChatAction.typing));
            boolean success = this.assistantService.sendRequest(bot.getAssistantId(), chatId, ",", text);
            if (success) {
                this.chatDataService.save(new ChatTempData(chatId, messageId, bot.getExecutableBot(), ResponseType.TEXT));
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e); //todo add here more cases and log.errors etc
        }
    }
}
