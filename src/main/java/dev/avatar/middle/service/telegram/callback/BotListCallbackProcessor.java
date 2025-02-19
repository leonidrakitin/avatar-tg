package dev.avatar.middle.service.telegram.callback;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.Language;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.TelegramUserBotSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotListCallbackProcessor extends TelegramCallbackProcessor {

    private final TelegramUserBotSettingsService telegramUserBotSettingsService;

    private final InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
            new InlineKeyboardButton("Apply now").url("http://ec2-16-16-104-222.eu-north-1.compute.amazonaws.com/")
    );

    public BotListCallbackProcessor(
            ChatDataService chatDataService,
            TelegramUserBotSettingsService telegramUserBotSettingsService
    ) {
        super(chatDataService);
        this.telegramUserBotSettingsService = telegramUserBotSettingsService;
    }

    @Override
    public CallbackType getCallbackType() {
        return CallbackType.BOT_LIST;
    }

    @Override
    public void process(TelegramBot bot, CallbackQuery callback) {
        long chatId = callback.message().chat().id();
        String botToken = bot.getToken();
        if (callback.data().equals("addNew")) {
//            createNew();
            return;
        }
        Language languageCode = ;
        this.telegramUserBotSettingsService.createIfNotExists(botToken, chatId, languageCode.toString().toLowerCase());
        bot.execute(new AnswerCallbackQuery(callback.id()));
        bot.execute(new DeleteMessage(chatId, callback.message().messageId()));
        bot.execute(new SendMessage(
                        chatId,
                        String.format("""
                            ✅ Preferred response language set to: %s.
                            However you should know that bot response firstly depends on your request text language.
                            
                            If you want to try create your own avatar, you can submit this form:
                            """,
                            languageCode.getLanguage()
                        )
                ).replyMarkup(keyboard)
        );
    }
}
