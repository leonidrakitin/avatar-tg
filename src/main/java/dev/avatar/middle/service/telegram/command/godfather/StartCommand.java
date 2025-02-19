package dev.avatar.middle.service.telegram.command.godfather;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.entity.TelegramUserEntity;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.service.TelegramUserService;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("godfatherStartCommand")
@RequiredArgsConstructor
public class StartCommand implements TelegramCommand {

    private final TelegramUserService telegramUserService;
    private final WhiteListService whiteListService;

    private final InlineKeyboardMarkup applyNowKeyboard = new InlineKeyboardMarkup(
            new InlineKeyboardButton("Apply now").url("http://ec2-16-16-104-222.eu-north-1.compute.amazonaws.com/")
    );


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
        boolean hasUserAccess = this.telegramUserService.findByChatId(chatId)
                .map(TelegramUserEntity::getTelegramUserId)
                .map(this.whiteListService::get)
                .isPresent();

        if (!hasUserAccess) {
            SendMessage message =
                    new SendMessage(chatId, "If you want to try create your own avatar, you can submit this form:")
                            .replyMarkup(applyNowKeyboard)
                            .parseMode(ParseMode.Markdown);
            telegramBot.getExecutableBot().execute(message);
            return;
        }

        //todo show menu


        // if (whitelist) -> apply now
        //
        // - admin (if botEntity.admin == chatId/telegramId)
        // -> /whitelist (long, username)
        // 1. @username -> @botUserName
        // 2. ..
        // 3. ..
        // -> add / remove  -> save / delete
        //
        // /menu --> List (edit), 'add new +'
        //
        // - user
        // -> Create Avatar (делаем по шагам)
        // --> 0. bot token id
        // --> 1. Create assistant
        // --> 2. Add heygen avatar ID
        // --> 3. Add elevan labs voice ID
        //
        // --> List
        //
        // -> Edit Avatar (by botToken)
        // --> Edit description
        // --> Edit File
        // ---> (fileIds) -> 1. 2. 3. -> add / remove
        // --> Edit heygen avatar ID
        // --> Edit elevan labs voice ID
    }
}
