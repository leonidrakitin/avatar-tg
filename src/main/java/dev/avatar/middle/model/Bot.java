package dev.avatar.middle.model;

import com.pengrad.telegrambot.TelegramBot;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
abstract public class Bot {

    private final String token;
    private final String assistantId; //todo hide from here
    private final TelegramBot executableBot;
    private final TelegramBotType botType;
    private final List<TelegramCommand> commands;

    public Bot(
            String token,
            String assistantId,
            TelegramBot executableBot,
            TelegramBotType botType,
            List<TelegramCommand> commands
    ) {
        this.token = token;
        this.assistantId = assistantId;
        this.botType = botType;
        this.commands = commands;
        this.executableBot = executableBot;
    }
}
