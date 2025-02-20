package dev.avatar.middle.service.telegram.callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.service.ChatDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class TelegramCallbackProcessor {

    public final ChatDataService chatDataService;

    public void processCallback(TelegramBot telegramBot, CallbackQuery callback) throws JsonProcessingException {
        log.info("Processing telegram callback: {}", this.getCallbackType().toString());
        this.process(telegramBot, callback);
        telegramBot.execute(new DeleteMessage(callback.message().chat().id(), callback.message().messageId()));
        log.info("Successfully finished processing telegram callback: {}", this.getCallbackType().toString());
    }

    public abstract CallbackType getCallbackType();

    abstract void process(TelegramBot telegramBot, CallbackQuery callback) throws JsonProcessingException;
}