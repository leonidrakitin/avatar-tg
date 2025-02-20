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
import dev.avatar.middle.entity.TelegramBotEntity;
import dev.avatar.middle.entity.TelegramUserEntity;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyBotsCommand implements TelegramCommand {

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
        return "/mybots";
    }

    @Override
    public void processCommand(Bot telegramBot, Long chatId) throws JsonProcessingException {

        Optional<TelegramUserEntity> admin =
                this.telegramUserRepository.findByChatIdAndAccessToUpDaily(chatId, true);

        if (admin.isEmpty()) {
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton("Apply now")
                            .url("http://ec2-16-16-104-222.eu-north-1.compute.amazonaws.com/"),
                    new InlineKeyboardButton("Check out our demo")
                            .url("https://t.me/updaily_demo_bot")
            );
            telegramBot.getExecutableBot().execute(new SendMessage(
                            chatId,
                            String.format("⭐\uFE0F If you want to try create your own avatar, you can submit this form:")
                    ).replyMarkup(keyboard)
            );
            return;
        }

        List<TelegramBotEntity> telegramBots = this.telegramBotRepository.findByAdminAndBotType(
                admin.get(),
                TelegramBotType.CLIENT_BOT
        );

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        telegramBots.forEach(tgBot -> {
            try {
                keyboard.addRow(
                        new InlineKeyboardButton(tgBot.getName())
                                .callbackData(this.callbackDataRepository.save(CallbackDataEntity.of(
                                        objectMapper.writeValueAsString(new BotActionDto(
                                                BotAction.GET.toString(), tgBot.getBotTokenId()
                                        ))
                                )).getId().toString())
                );
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        });

        keyboard.addRow(new InlineKeyboardButton("➕ Create new avatar")
                .callbackData(this.callbackDataRepository.save(CallbackDataEntity.of(
                        objectMapper.writeValueAsString(new BotActionDto(
                                BotAction.CREATE.toString(), null
                        ))
                )).getId().toString())
        );

        SendResponse response = telegramBot.getExecutableBot()
                .execute(new SendMessage(chatId, "\uD83E\uDEC2 This your avatars:")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard)
        );
        CallbackBotEntity callbackBotEntity = CallbackBotEntity.builder()
                .id(UUID.randomUUID())
                .botTokenId(telegramBot.getToken())
                .callbackMessageId(response.message().messageId())
                .callbackType(CallbackType.BOT_LIST)
                .build();

        this.callbackBotRepository.save(callbackBotEntity);

    }
}
