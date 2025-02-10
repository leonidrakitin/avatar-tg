package dev.avatar.middle.service.telegram.command.client;

import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StartCommand implements TelegramCommand {

    @Override
    public TelegramBotType getBotType() {
        return TelegramBotType.CLIENT_BOT;
    }

    @Override
    public String getDescription() {
        return "Start bot command";
    } //todo i18n

    @Override
    public String getCommand() {
        return "/start";
    }

    @Override
    public void processCommand(Bot telegramBot, Long chatId) {
        List<BotCommand> botCommands = telegramBot.getCommands().stream()
                .map(cmd -> new BotCommand(cmd.getCommand(), cmd.getDescription()))
                .toList();
        SetMyCommands helpCommands = new SetMyCommands(botCommands.toArray(BotCommand[]::new));
        telegramBot.getExecutableBot().execute(helpCommands);
        //todo sent to assistant message "Hi, tell who are you? Respone in language_code: %s" i18n
        SendMessage message = new SendMessage(chatId, "Hi! How can I assist you today?");
        telegramBot.getExecutableBot().execute(message);
    }
}
