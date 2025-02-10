package dev.avatar.middle.service.telegram.callback;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.ChatTempData;
import dev.avatar.middle.service.ChatDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class TelegramCallbackProcessor {

    public final ChatDataService chatDataService;

    public void processCallback(TelegramBot telegramBot, CallbackQuery callback) {
        log.info("Processing telegram callback: {}", this.getCallbackType().toString());
        ChatTempData chatTempData = this.chatDataService.get(telegramBot.getToken(), callback.message().chat().id()) //todo deprecated
                .orElseThrow(() ->
                        new RuntimeException("Unexpected behavior, chat data not found for chat id " +
                                callback.message().chat().id())
                ); //todo chatdataexceptions
        this.process(telegramBot, callback, chatTempData);
        this.clearData(chatTempData);
        log.info("Successfully finished processing telegram callback: {}", this.getCallbackType().toString());
    }

    //todo clearCallbackData and clearMessageData move to service chat data
    public void clearData(ChatTempData chatTempData) {
        chatDataService.clearCallbackData(chatTempData);
    }

    public abstract CallbackType getCallbackType();

    abstract void process(TelegramBot telegramBot, CallbackQuery callback, ChatTempData chatTempData);
}