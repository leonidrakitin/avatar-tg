package dev.avatar.middle.service.telegram.callback;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.TelegramUserBotSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommunicationTypeCallbackProcessor extends TelegramCallbackProcessor {

    private final TelegramUserBotSettingsService telegramUserBotSettingsService;

    public CommunicationTypeCallbackProcessor(
            ChatDataService chatDataService,
            TelegramUserBotSettingsService telegramUserBotSettingsService
    ) {
        super(chatDataService);
        this.telegramUserBotSettingsService = telegramUserBotSettingsService;
    }

    @Override
    public CallbackType getCallbackType() {
        return CallbackType.CommunicationTypeCallback;
    }

    @Override
    public void process(TelegramBot bot, CallbackQuery callback) {
        long chatId = callback.message().chat().id();
        String botToken = bot.getToken();
        ResponseType responseType = ResponseType.valueOf(callback.data());
        this.telegramUserBotSettingsService.updateUserResponseType(botToken, chatId, responseType);
        bot.execute(new AnswerCallbackQuery(callback.id()));
        bot.execute(new SendMessage(chatId, "✅ Response type set to: " + responseType));
    }
}
