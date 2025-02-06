package dev.avatar.middle.service.telegram.command;

import com.pengrad.telegrambot.TelegramBot;

public interface TelegramCommand {

    String getDescription();
    String getCommand();
    void processCommand(TelegramBot telegramBot, Long chatId);
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