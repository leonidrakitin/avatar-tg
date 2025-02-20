package dev.avatar.middle.service.telegram.command.godfather;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.entity.CallbackBotEntity;
import dev.avatar.middle.entity.CallbackDataEntity;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.BotAction;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.CallbackBotRepository;
import dev.avatar.middle.repository.CallbackDataRepository;
import dev.avatar.middle.repository.TelegramBotRepository;
import dev.avatar.middle.repository.TelegramUserRepository;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import dev.avatar.middle.service.telegram.command.dto.BotActionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component("godfatherBot")
@RequiredArgsConstructor
public class StartCommand implements TelegramCommand {

    private final ObjectMapper objectMapper;
    private final CallbackBotRepository callbackBotRepository;
    private final CallbackDataRepository callbackDataRepository;
    private final TelegramBotRepository telegramBotRepository;
    private final TelegramUserRepository telegramUserRepository;

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
    public void processCommand(Bot telegramBot, Long chatId) throws JsonProcessingException {

        SendResponse response = telegramBot.getExecutableBot()
                .execute(new SendMessage(chatId, "⭐\uFE0F ***UpDaily platform v1.0.0***\n\nPress ***start***")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(new InlineKeyboardMarkup(
                        new InlineKeyboardButton("Start")
                                .callbackData(this.callbackDataRepository.save(
                                        CallbackDataEntity.of(objectMapper.writeValueAsString(
                                                new BotActionDto(BotAction.LIST.toString(), null)
                                        ))
                                ).getId().toString())
                ))
        );

        CallbackBotEntity callbackBotEntity = CallbackBotEntity.builder()
                .id(UUID.randomUUID())
                .botTokenId(telegramBot.getToken())
                .callbackMessageId(response.message().messageId())
                .callbackType(CallbackType.BOT_LIST)
                .build();
        this.callbackBotRepository.save(callbackBotEntity);

        boolean hasAdminAccess =
                this.telegramUserRepository.findByChatIdAndAccessToUpDaily(chatId, true)
                        .map(user -> telegramBotRepository.findByAdminAndBotType(user, TelegramBotType.GODFATHER_BOT))
                        .isPresent();

        List<BotCommand> botCommands = telegramBot.getCommands().stream()
                .filter(cmd -> !cmd.getCommand().equals("/whitelist") || hasAdminAccess)
                .map(cmd -> new BotCommand(cmd.getCommand(), cmd.getDescription()))
                .toList();
        SetMyCommands helpCommands = new SetMyCommands(botCommands.toArray(BotCommand[]::new));
        telegramBot.getExecutableBot().execute(helpCommands);
    }
}
