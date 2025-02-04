package dev.avatar.middle.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import dev.avatar.middle.conf.AppProperty;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final AppProperty properties;
    private final TelegramRequestService telegramRequestService;
    private final List<TelegramBot> telegramBots = new ArrayList<>();

    @PostConstruct
    public void init() {
        telegramBots.add(new TelegramBot(properties.getToken()));
        for (TelegramBot bot : telegramBots) {
            bot.setUpdatesListener((List<Update> updates) -> {
                for (Update update : updates) {
                    telegramRequestService.processRequest(bot, update);
                }
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }, e -> {
                //todo global error handler  ???
                if (e.response() != null) {
                    log.error("Catch error: {}, description: {}", e.response().errorCode(), e.response().description());
                }
                else {
                    log.error("Unexpected error: {}", e.getMessage());
                }
            });
        }
    }

    //todo create, edit, update bot
}
