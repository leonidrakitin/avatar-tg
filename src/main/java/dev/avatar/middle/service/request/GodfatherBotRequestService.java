package dev.avatar.middle.service.request;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.CallbackBotRepository;
import dev.avatar.middle.repository.TelegramBotRepository;
import dev.avatar.middle.repository.TelegramUserRepository;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.TelegramFileService;
import dev.avatar.middle.service.ai.AssistantService;
import dev.avatar.middle.service.telegram.callback.TelegramCallbackProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class GodfatherBotRequestService extends AbstractBotRequestService {

    private final AssistantService assistantService;
    private final TelegramFileService telegramFileService;
    private final TelegramUserRepository telegramUserRepository;
    private final TelegramBotRepository telegramBotRepository;
    private final ChatDataService chatDataService;

    public GodfatherBotRequestService(
            ChatDataService chatDataService,
            List<TelegramCallbackProcessor> callbacks,
            AssistantService assistantService,
            TelegramFileService telegramFileService,
            TelegramUserRepository telegramUserRepository,
            TelegramBotRepository telegramBotRepository,
            CallbackBotRepository callbackBotRepository
    ) {
        super(chatDataService, callbackBotRepository, callbacks);
        this.assistantService = assistantService;
        this.telegramUserRepository = telegramUserRepository;
        this.telegramFileService = telegramFileService;
        this.telegramBotRepository = telegramBotRepository;
        this.chatDataService = chatDataService;
    }

    @Override
    public void handleMessageUpdate(Bot bot, Message message) {
        long chatId = message.chat().id();
        if (message.text() != null) {
            chatDataService.get(bot.getToken(), chatId)
                    .filter(chatTempData -> chatTempData.getAction().equals("whitelist"))
                    .ifPresent(chatTempData -> {
                        try {
                            Long id = Long.valueOf(message.text());
                            this.telegramUserRepository.findById(id).ifPresent(user -> {
                                this.telegramUserRepository.save(user.toBuilder().accessToUpDaily(true).build());
                                bot.getExecutableBot().execute(new SendMessage(
                                        chatId,
                                        "✅ Successfully granted access to @" + user.getUsername()
                                ));
                            });
                        } catch (Exception e) {
                            log.error(e.getMessage());
                        }
                    });
        } else if (message.document() != null) {
            this.processDocument(bot, chatId, message.document().fileName(), message.document().fileId());
        }
    }

    @Override
    public TelegramBotType getSupportedBotType() {
        return TelegramBotType.CLIENT_BOT;
    }

    private void processDocument(Bot bot, long chatId, String fileName, String fileId) {
        String vectorStoreId = this.telegramBotRepository.findByBotTokenId(bot.getToken())
                .orElseThrow(() -> new RuntimeException("Unexpected behaviour"))
                .getVectorStoreId();

        byte[] fileData = this.telegramFileService.getTelegramFile(bot.getExecutableBot(), fileId);
        this.assistantService.processDocument(vectorStoreId, fileName, fileData);
        bot.getExecutableBot().execute(new SendMessage(chatId, "⌛\uFE0F Your file is uploading and will appear in several minutes.."));
    }
}
