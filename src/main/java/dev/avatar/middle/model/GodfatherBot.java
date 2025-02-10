package dev.avatar.middle.model;

import com.pengrad.telegrambot.TelegramBot;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.Getter;

import java.util.List;

@Getter
public class GodfatherBot extends Bot {

    public GodfatherBot(
            String token,
            String assistantId,
            TelegramBot executableBot,
            TelegramBotType botType,
            List<TelegramCommand> commands
    ) {
        super(token, assistantId, executableBot, botType, commands);
    }
}