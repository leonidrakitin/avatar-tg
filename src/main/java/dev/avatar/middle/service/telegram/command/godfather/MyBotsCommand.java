//package dev.avatar.middle.service.telegram.command.godfather;
//
//import com.pengrad.telegrambot.model.BotCommand;
//import com.pengrad.telegrambot.model.request.ParseMode;
//import com.pengrad.telegrambot.request.SendMessage;
//import com.pengrad.telegrambot.request.SetMyCommands;
//import dev.avatar.middle.model.Bot;
//import dev.avatar.middle.model.TelegramBotType;
//import dev.avatar.middle.service.telegram.command.TelegramCommand;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class MyBotsCommand implements TelegramCommand {
//
//    @Override
//    public TelegramBotType getBotType() {
//        return TelegramBotType.GODFATHER_BOT;
//    }
//
//    @Override
//    public String getDescription() {
//        return "List of my bots";
//    } //todo i18n
//
//    @Override
//    public String getCommand() {
//        return "/mybots";
//    }
//
//    @Override
//    public void processCommand(Bot telegramBot, Long chatId) {
//
//        SetMyCommands helpCommands = new SetMyCommands(botCommands.toArray(BotCommand[]::new));
//        telegramBot.getExecutableBot().execute(helpCommands);
//        SendMessage message = new SendMessage(chatId, "Hi! I'm **Updaily** Bot.").parseMode(ParseMode.Markdown);
//        telegramBot.getExecutableBot().execute(message);
//    }
//}
