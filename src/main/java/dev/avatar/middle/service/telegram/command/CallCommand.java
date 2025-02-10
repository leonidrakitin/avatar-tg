package dev.avatar.middle.service.telegram.command;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.entity.HeyGenAvatar;
import dev.avatar.middle.entity.TelegramUserBotSettingsEntity;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.HeyGenAvatarRepository;
import dev.avatar.middle.service.TelegramUserBotSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallCommand implements TelegramCommand {

    private final HeyGenAvatarRepository heyGenAvatarRepository;
    private final TelegramUserBotSettingsService telegramUserBotSettingsService;

    @Override
    public TelegramBotType getBotType() {
        return TelegramBotType.CLIENT_BOT;
    }

    @Override
    public String getDescription() {
        return "Start call";
    }

    @Override
    public String getCommand() {
        return "/call";
    }

    @Override
    public void processCommand(Bot telegramBot, Long chatId) {

        HeyGenAvatar avatarData = this.heyGenAvatarRepository.findByBotTokenId(telegramBot.getToken())
                .orElseThrow(); //todo add exception and global handler

        TelegramUserBotSettingsEntity settings =
                this.telegramUserBotSettingsService.createIfNotExists(telegramBot.getToken(), chatId);

        //todo url 'https://call-v2.vercel...' should property
        String url = String.format(
                "https://call-v2.vercel.app/?chatid=%s&avatar_id=%s&avatar_voice_id=%s&assistant_id=%s&language=%s",
                chatId,
                avatarData.getAvatarId(),
                avatarData.getVoiceId(),
                telegramBot.getAssistantId(),
                settings.getLanguageCode()
        );
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(new InlineKeyboardButton("Start call").url(url));

        //todo hide execution logic from here
        SendMessage message = new SendMessage(chatId, "Press button to start video call.").replyMarkup(keyboard);
        telegramBot.getExecutableBot().execute(message);
    }
}
