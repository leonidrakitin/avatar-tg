package dev.avatar.middle.service.telegram.command.client;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.ChatTempData;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunicationTypeCommand implements TelegramCommand {

    //todo i18n, localization
    private final InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
            new InlineKeyboardButton("TEXT").callbackData(ResponseType.TEXT.toString()),  //todo i18n
            new InlineKeyboardButton("VIDEO").callbackData(ResponseType.VIDEO.toString()),  //todo i18n
            new InlineKeyboardButton("VOICE").callbackData(ResponseType.VOICE.toString()));  //todo i18n

    private final ChatDataService chatDataService;

    @Override
    public TelegramBotType getBotType() {
        return TelegramBotType.CLIENT_BOT;
    }

    @Override
    public String getDescription() {
        return "\uD83D\uDDE3 Change communication type"; //todo i18n
    }

    @Override
    public String getCommand() {
        return "/type";
    }

    @Override
    public void processCommand(Bot telegramBot, Long chatId) {
        ChatTempData chatTempData = this.chatDataService.get(telegramBot.getToken(), chatId)
                .orElseGet(() -> new ChatTempData(chatId, telegramBot.getExecutableBot()));
        chatTempData.setCallbackType(CallbackType.CommunicationTypeCallback);
        this.chatDataService.save(chatTempData);
        SendMessage message = new SendMessage(
                chatId,
                """
                    Pick a format for my answers. I can reply to you in text,
                    voice messages, and Circle Video format. You can always change
                    this by pressing the \"menu\" button.
                    """
        ).replyMarkup(keyboard);
        telegramBot.getExecutableBot().execute(message);
    }
}
