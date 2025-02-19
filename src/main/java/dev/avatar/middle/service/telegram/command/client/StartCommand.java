package dev.avatar.middle.service.telegram.command.client;

import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.entity.CallbackBotEntity;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.Language;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.CallbackBotRepository;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("clientStartCommand")
@RequiredArgsConstructor
public class StartCommand implements TelegramCommand {

    private final CallbackBotRepository callbackBotRepository;

    private final InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup()
            .addRow(
                    new InlineKeyboardButton(Language.RU.getLanguage()).callbackData(Language.RU.toString()),
                    new InlineKeyboardButton(Language.EN.getLanguage()).callbackData(Language.EN.toString())
            )
            .addRow(
                    new InlineKeyboardButton(Language.DE.getLanguage()).callbackData(Language.DE.toString()),
                    new InlineKeyboardButton(Language.AR.getLanguage()).callbackData(Language.AR.toString())
            )
            .addRow(
                    new InlineKeyboardButton(Language.ZH.getLanguage()).callbackData(Language.ZH.toString()),
                    new InlineKeyboardButton(Language.IT.getLanguage()).callbackData(Language.IT.toString())
            )
            .addRow(
                    new InlineKeyboardButton(Language.FR.getLanguage()).callbackData(Language.FR.toString()),
                    new InlineKeyboardButton(Language.ES.getLanguage()).callbackData(Language.ES.toString())
            )
            .addRow(
                    new InlineKeyboardButton(Language.HI.getLanguage()).callbackData(Language.HI.toString()),
                    new InlineKeyboardButton(Language.TR.getLanguage()).callbackData(Language.TR.toString())
            )
            .addRow(
                    new InlineKeyboardButton(Language.JA.getLanguage()).callbackData(Language.JA.toString()),
                    new InlineKeyboardButton(Language.PT.getLanguage()).callbackData(Language.PT.toString())
            );

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
        SendMessage message = new SendMessage(chatId, """
                👩🏼‍💼 I’m Evgenia Romanova, your transformational mentor. I’m here to support you in aligning with your true self, trusting your inner impulses, and harmonizing with the world around you. Think of me as a guide on your journey of self-discovery. How can I assist you today?

                🔹 Use ***/type*** to choose your preferred communication method: VOICE 🎙 / TEXT 💬 / VIDEO CIRCLE 📹
                🔹 Use ***/call*** to create a meeting room with me.
                🔹 Use ***/cancel*** to cancel previous request.

                🔹 You can also access this by clicking the ***MENU*** button on the left side of the text input.

                I'm working on UpDaily platform.
 
                - Please choose language:
                """)
                .replyMarkup(keyboard)
                .parseMode(ParseMode.Markdown);
        SendResponse response = telegramBot.getExecutableBot().execute(message);
        CallbackBotEntity callbackBotEntity = CallbackBotEntity.builder()
                .id(UUID.randomUUID())
                .botTokenId(telegramBot.getToken())
                .callbackMessageId(response.message().messageId())
                .callbackType(CallbackType.LANGUAGE)
                .build();
        this.callbackBotRepository.save(callbackBotEntity);
    }
}
