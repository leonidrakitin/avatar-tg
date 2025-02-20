package dev.avatar.middle.service.telegram.callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.entity.CallbackDataEntity;
import dev.avatar.middle.entity.TelegramUserEntity;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.ChatTempData;
import dev.avatar.middle.repository.CallbackDataRepository;
import dev.avatar.middle.repository.TelegramUserRepository;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.telegram.command.dto.UserAccessDto;
import dev.avatar.middle.service.telegram.command.godfather.WhiteListCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class WhiteListCallbackProcessor extends TelegramCallbackProcessor {

    private final CallbackDataRepository callbackDataRepository;
    private final TelegramUserRepository telegramUserRepository;
    private final WhiteListCommand whiteListCommand;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WhiteListCallbackProcessor(
            ChatDataService chatDataService,
            CallbackDataRepository callbackDataRepository,
            TelegramUserRepository telegramUserRepository,
            WhiteListCommand whiteListCommand
    ) {
        super(chatDataService);
        this.callbackDataRepository = callbackDataRepository;
        this.telegramUserRepository = telegramUserRepository;
        this.whiteListCommand = whiteListCommand;
    }

    @Override
    public CallbackType getCallbackType() {
        return CallbackType.WHITELIST;
    }

    @Override
    public void process(TelegramBot bot, CallbackQuery callback) throws JsonProcessingException {
        long chatId = callback.message().chat().id();
        CallbackDataEntity callbackData = this.callbackDataRepository.findById(UUID.fromString(callback.data()))
                .orElseThrow(() -> new RuntimeException("Unexpected behaviour"));
        UserAccessDto userAccessDto = objectMapper.readValue(callbackData.getData(), UserAccessDto.class);

        if (userAccessDto.userId() != null) {
            var telegramUser = this.telegramUserRepository.findById(userAccessDto.userId())
                    .map(TelegramUserEntity::toBuilder)
                    .map(user -> user.accessToUpDaily(false))
                    .map(TelegramUserEntity.TelegramUserEntityBuilder::build)
                    .map(this.telegramUserRepository::save)
                    .orElseThrow(() -> new RuntimeException("Unexpected behaviour"));
            bot.execute(new SendMessage(chatId, "✅ Successfully removed access from " + telegramUser.getUsername()));
            bot.execute(new AnswerCallbackQuery(callback.id()));
            bot.execute(new DeleteMessage(chatId, callback.message().messageId()));
        } else {
            chatDataService.get(bot.getToken(), chatId)
                    .orElseGet(() -> this.chatDataService.save(new ChatTempData(chatId, bot, "whitelist")));
            bot.execute(new SendMessage(chatId, "⭐\uFE0F Enter telegram ID to grant access"));
        }
    }
}
