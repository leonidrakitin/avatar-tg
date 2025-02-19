package dev.avatar.middle.service.telegram.command.godfather;

import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("godfatherStartCommand")
@RequiredArgsConstructor
public class StartCommand implements TelegramCommand {

    private final InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
            new InlineKeyboardButton("🇬🇧 English").callbackData(ResponseType.TEXT.toString()),
            new InlineKeyboardButton("🇷🇺 Русский").callbackData(ResponseType.VIDEO.toString()),
            new InlineKeyboardButton("🇩🇪 Deutsch").callbackData(ResponseType.TEXT.toString()),
            new InlineKeyboardButton("🇸🇦 العربية").callbackData(ResponseType.TEXT.toString()),
            new InlineKeyboardButton("🇨🇳 中文").callbackData(ResponseType.VOICE.toString()),
            new InlineKeyboardButton("🇮🇹 Italiano").callbackData(ResponseType.TEXT.toString()),
            new InlineKeyboardButton("🇫🇷 Français").callbackData(ResponseType.VIDEO.toString()),
            new InlineKeyboardButton("🇪🇸 Español").callbackData(ResponseType.VOICE.toString()),
            new InlineKeyboardButton("🇮🇳 हिन्दी").callbackData(ResponseType.TEXT.toString()),
            new InlineKeyboardButton("🇹🇷 Türkçe").callbackData(ResponseType.TEXT.toString()),
            new InlineKeyboardButton("🇯🇵 日本語").callbackData(ResponseType.VIDEO.toString()),
            new InlineKeyboardButton("🇵🇹 Português").callbackData(ResponseType.VOICE.toString()));

    @Override
    public TelegramBotType getBotType() {
        return TelegramBotType.GODFATHER_BOT;
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
        SendMessage message = new SendMessage(chatId, "Hi! I'm **Updaily** Bot. Please choose preferred language for bot.")
                .replyMarkup(keyboard)
                .parseMode(ParseMode.Markdown);
        telegramBot.getExecutableBot().execute(message);
    }
}
