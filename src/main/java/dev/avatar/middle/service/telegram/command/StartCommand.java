package dev.avatar.middle.service.telegram.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.service.ai.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StartCommand implements TelegramCommand {

    private final AssistantService assistantService;
    private final List<TelegramCommand> commands;
    //todo i18n, localization
    private final InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
            new InlineKeyboardButton("TEXT").callbackData(ResponseType.TEXT.toString()),
            new InlineKeyboardButton("VIDEO").callbackData(ResponseType.VIDEO.toString()));

    @Override
    public String getDescription() {
        return "Start bot command";
    } //todo i18n

    @Override
    public String getCommand() {
        return "/start";
    }

    @Override
    public void processCommand(TelegramBot telegramBot, Long telegramUserId) {
        List<BotCommand> botCommands = commands.stream()
                .map(cmd -> new BotCommand(cmd.getCommand(), cmd.getDescription()))
                .toList();
        SetMyCommands helpCommands = new SetMyCommands(botCommands.toArray(BotCommand[]::new));
        telegramBot.execute(helpCommands);
        //todo sent to assistant message "Hi, tell who are you? Respone in language_code: %s"
        SendMessage message = new SendMessage(telegramUserId, "Hi!");
        telegramBot.execute(message);
    }
}
