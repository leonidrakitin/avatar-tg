package dev.avatar.middle.service.telegram.command.client;

import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelCommand implements TelegramCommand {

    private final ChatDataService chatDataService;

    @Override
    public TelegramBotType getBotType() {
        return TelegramBotType.CLIENT_BOT;
    }

    @Override
    public String getDescription() {
        return "Cancel active request";
    } //todo i18n

    @Override
    public String getCommand() {
        return "/cancel";
    }

    @Override
    public void processCommand(Bot telegramBot, Long chatId) {
        this.chatDataService.clearMessageData(telegramBot.getToken(), chatId);
        SendMessage message = new SendMessage(chatId, "❌ Canceled previous request"); //todo i18n
        telegramBot.getExecutableBot().execute(message);
    }
}
