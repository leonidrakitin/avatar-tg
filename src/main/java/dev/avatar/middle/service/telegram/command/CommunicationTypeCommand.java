package dev.avatar.middle.service.telegram.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.ChatData;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.service.ChatDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunicationTypeCommand implements TelegramCommand {

    //todo i18n, localization
    private final InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
            new InlineKeyboardButton("TEXT").callbackData(ResponseType.TEXT.toString()),
            new InlineKeyboardButton("VIDEO").callbackData(ResponseType.VIDEO.toString()));
    private final ChatDataService chatDataService;

    @Override
    public String getDescription() {
        return "\uD83D\uDDE3 Change communication type";
    }

    @Override
    public String getCommand() {
        return "/type";
    }

    @Override
    public void processCommand(TelegramBot telegramBot, Long chatId) {
        ChatData chatData = this.chatDataService.getByChatId(chatId);
        chatData.setCallbackType(CallbackType.CommunicationTypeCallback);
        this.chatDataService.save(chatData);
        SendMessage message = new SendMessage(
                chatId,
                """
                        Pick a format for my answers. I can reply to you in text,
                        voice messages, and Circle Video format. You can always change
                        this by pressing the \"menu\" button.
                    """
        ).replyMarkup(keyboard);
        telegramBot.execute(message);
    }
}
