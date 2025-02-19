package dev.avatar.middle.service.telegram.command.client;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.entity.CallbackBotEntity;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.CallbackBotRepository;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunicationTypeCommand implements TelegramCommand {

    private final CallbackBotRepository callbackBotRepository;

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
        SendMessage message = new SendMessage(
                chatId,
                """
                    Pick a format for my answers. I can reply to you in text,
                    voice messages, and Circle Video format. You can always change
                    this by pressing the \"menu\" button.
                    """
        ).replyMarkup(keyboard);
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
