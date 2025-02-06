package dev.avatar.middle.service.telegram.callback;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.ChatData;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.TelegramUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommunicationTypeCallbackProcessor extends TelegramCallbackProcessor {

    private final TelegramUserService telegramUserService;

    public CommunicationTypeCallbackProcessor(
            ChatDataService chatDataService,
            TelegramUserService telegramUserService
    ) {
        super(chatDataService);
        this.telegramUserService = telegramUserService;
    }

    @Override
    public CallbackType getCallbackType() {
        return CallbackType.CommunicationTypeCallback;
    }

    @Override
    public void process(TelegramBot bot, CallbackQuery callback, ChatData chatData) {
        long chatId = chatData.getChatId(); //todo message is deprecated
        long telegramUserId = callback.from().id();
        ResponseType responseType = ResponseType.valueOf(callback.data());
        this.telegramUserService.updateUserResponseType(telegramUserId, responseType);
        bot.execute(new AnswerCallbackQuery(callback.id()));
        bot.execute(new SendMessage(chatId, "✅ Response type set to: " + responseType));
    }
}
