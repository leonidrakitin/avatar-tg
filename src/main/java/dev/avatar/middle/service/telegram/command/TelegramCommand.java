package dev.avatar.middle.service.telegram.command;

import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.TelegramBotType;

public interface TelegramCommand {

    TelegramBotType getBotType();
    String getDescription();
    String getCommand();
    void processCommand(Bot telegramBot, Long chatId);
}

/**
 * TODO implement
 *         return new SetMyCommands(
 *                 new BotCommand("/type", "\uD83D\uDDE3 Change communication type"),
 *                 new BotCommand("/myprofile", "\uD83D\uDC64 Check my profile"),
 *                 new BotCommand("/summarize", "☝\uFE0F Summarize last meeting"),
 *                 new BotCommand("/language", "\uD83D\uDCAC Change language")
 *         );
 */