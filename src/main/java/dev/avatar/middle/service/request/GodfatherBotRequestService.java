package dev.avatar.middle.service.request;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.SendChatAction;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.ChatTempData;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.CallbackBotRepository;
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
            TelegramUserService telegramUserService,
            CallbackBotRepository callbackBotRepository
    ) {
        super(chatDataService, callbackBotRepository, callbacks);
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

    }

    @Override
    public TelegramBotType getSupportedBotType() {
        return TelegramBotType.CLIENT_BOT;
    }

    private void processVoiceMessage(
            Bot bot,
            int messageId,
            String fileId,
            long chatId
    ) {
        byte[] fileData = this.telegramFileService.getTelegramFile(bot.getExecutableBot(), fileId);
        String transcribedAudio = this.assistantService.transcriptAudio(fileData, "en");
        log.debug("Got result from transcription audio service: {}", transcribedAudio); //todo i18n
        this.sendRequest(bot, messageId, transcribedAudio, chatId);
    }

    private void processDocument(Bot bot, long chatId, String fileId, String content) throws ExecutionException {
        byte[] fileData = this.telegramFileService.getTelegramFile(bot.getExecutableBot(), fileId);
        this.assistantService.processDocument(bot.getToken(), bot.getAssistantId(), chatId, fileData, content);
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
