package dev.avatar.middle.service.telegram.command.godfather;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.entity.CallbackBotEntity;
import dev.avatar.middle.entity.CallbackDataEntity;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.CallbackBotRepository;
import dev.avatar.middle.repository.CallbackDataRepository;
import dev.avatar.middle.repository.TelegramBotRepository;
import dev.avatar.middle.repository.TelegramUserRepository;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import dev.avatar.middle.service.telegram.command.dto.UserAccessDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhiteListCommand implements TelegramCommand {

    private final CallbackBotRepository callbackBotRepository;
    private final CallbackDataRepository callbackDataRepository;
    private final TelegramUserRepository telegramUserRepository;
    private final TelegramBotRepository telegramBotRepository;
    private final ObjectMapper objectMapper;

    @Override
    public TelegramBotType getBotType() {
        return TelegramBotType.GODFATHER_BOT;
    }

    @Override
    public String getDescription() {
        return "List of my bots";
    } //todo i18n

    @Override
    public String getCommand() {
        return "/whitelist";
    }

    @Override
    public void processCommand(Bot telegramBot, Long chatId) throws JsonProcessingException {

        boolean hasAccess =
                this.telegramUserRepository.findByChatIdAndAccessToUpDaily(chatId, true)
                        .map(user -> telegramBotRepository.findByAdminAndBotType(user, TelegramBotType.GODFATHER_BOT))
                        .isPresent();

        if (!hasAccess) {
            telegramBot.getExecutableBot().execute(new SendMessage(chatId, "❌ You dont have access!"));
            return;
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        this.telegramUserRepository.findByAccessToUpDaily(true)
                .forEach(user -> {
                    try {
                        keyboardMarkup.addRow(new InlineKeyboardButton(
                                String.format("%s (%s)", user.getUsername(), user.getFirstName())
                        ).callbackData(
                                this.callbackDataRepository.save(
                                        CallbackDataEntity.of(objectMapper.writeValueAsString(
                                                new UserAccessDto(user.getTelegramUserId())
                                        ))
                                ).getId().toString()
                        ));
                    } catch (JsonProcessingException e) {
                        log.error(e.getMessage());
                    }
                });
        keyboardMarkup.addRow(new InlineKeyboardButton("➕ Add new user").callbackData(
                this.callbackDataRepository.save(
                        CallbackDataEntity.of(objectMapper.writeValueAsString(
                                new UserAccessDto(null)
                        ))
                ).getId().toString()
        ));
        SendResponse response = telegramBot.getExecutableBot()
                .execute(
                        new SendMessage(chatId, "✔\uFE0F All who has access:\n\n- Click to ***demote***")
                                .replyMarkup(keyboardMarkup)
                                .parseMode(ParseMode.Markdown)
                );

        CallbackBotEntity callbackBotEntity = CallbackBotEntity.builder()
                .id(UUID.randomUUID())
                .botTokenId(telegramBot.getToken())
                .callbackMessageId(response.message().messageId())
                .callbackType(CallbackType.WHITELIST)
                .build();
        this.callbackBotRepository.save(callbackBotEntity);
    }
}
