package dev.avatar.middle.service.telegram.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.service.ai.AssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallCommand implements TelegramCommand {

    private final AssistantService assistantService;

    @Override
    public String getDescription() {
        return "Start call";
    }

    @Override
    public String getCommand() {
        return "/call";
    }

    @Override
    public void processCommand(TelegramBot telegramBot, Long chatId) {
        String avatarId = "56af96effa464cedae632dc85f8d2984";
        String avatarVoiceId = "671d6729b8da48aba81152cc29a631e3";
        String assistantId = assistantService.getAssistantId(telegramBot.getToken());
        String language = "en";

        String url = String.format(
                "https://call-git-main-dimas-projects-5364656e.vercel.app/?chatid=%s&avatar_id=%s&avatar_voice_id=%s&assistant_id=%s&language=%s",
                chatId, avatarId, avatarVoiceId, assistantId, language
        );

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Start call").url(url));


        SendMessage message = new SendMessage(
                chatId,
                """
                        Press button to start video call.
                        """
        ).replyMarkup(keyboard);
        telegramBot.execute(message);
    }
}
