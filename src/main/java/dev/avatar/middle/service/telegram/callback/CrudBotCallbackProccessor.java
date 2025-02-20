package dev.avatar.middle.service.telegram.callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.entity.CallbackBotEntity;
import dev.avatar.middle.entity.CallbackDataEntity;
import dev.avatar.middle.entity.TelegramBotEntity;
import dev.avatar.middle.entity.TelegramUserEntity;
import dev.avatar.middle.model.BotAction;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.FilesAction;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.CallbackBotRepository;
import dev.avatar.middle.repository.CallbackDataRepository;
import dev.avatar.middle.repository.TelegramBotRepository;
import dev.avatar.middle.repository.TelegramUserRepository;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.TelegramUserService;
import dev.avatar.middle.service.ai.AssistantService;
import dev.avatar.middle.service.telegram.command.dto.BotActionDto;
import dev.avatar.middle.service.telegram.command.dto.FileActionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class CrudBotCallbackProccessor extends TelegramCallbackProcessor {

    private final AssistantService assistantService;
    private final CallbackBotRepository callbackBotRepository;
    private final CallbackDataRepository callbackDataRepository;
    private final TelegramBotRepository telegramBotRepository;
    private final TelegramUserRepository telegramUserRepository;
    private final TelegramUserService telegramUserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CrudBotCallbackProccessor(
            ChatDataService chatDataService,
            AssistantService assistantService,
            CallbackBotRepository callbackBotRepository,
            CallbackDataRepository callbackDataRepository,
            TelegramBotRepository telegramBotRepository,
            TelegramUserRepository telegramUserRepository,
            TelegramUserService telegramUserService
    ) {
        super(chatDataService);
        this.assistantService = assistantService;
        this.callbackBotRepository = callbackBotRepository;
        this.callbackDataRepository = callbackDataRepository;
        this.telegramBotRepository = telegramBotRepository;
        this.telegramUserRepository = telegramUserRepository;
        this.telegramUserService = telegramUserService;
    }

    @Override
    public CallbackType getCallbackType() {
        return CallbackType.BOT_LIST;
    }

    @Override
    public void process(TelegramBot bot, CallbackQuery callback) throws JsonProcessingException {
        long chatId = callback.message().chat().id();
        CallbackDataEntity callbackData = this.callbackDataRepository.findById(UUID.fromString(callback.data()))
                .orElseThrow(() -> new RuntimeException("Unexpected behaviour"));
        BotActionDto botActionDto = objectMapper.readValue(callbackData.getData(), BotActionDto.class);
        BotAction botAction = BotAction.valueOf(botActionDto.action());
        SendResponse response = null;

        if (botAction == BotAction.GET) {
            String botToken = botActionDto.botToken();
            TelegramBotEntity telegramBotEntity = this.telegramBotRepository.findByBotTokenId(botToken)
                    .orElseThrow(() -> new RuntimeException("Unexpected behaviour"));
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton("\uD83D\uDCDA Knowledge base")
                            .callbackData(this.callbackDataRepository.save(
                                    CallbackDataEntity.of(objectMapper.writeValueAsString(
                                            new FileActionDto(
                                                    FilesAction.LIST.toString(),
                                                    null,
                                                    telegramBotEntity.getVectorStoreId()
                                            )
                                    ))
                            ).getId().toString())
//                    new InlineKeyboardButton("↩ Back")
//                            .callbackData(this.callbackDataRepository.save(
//                                    CallbackDataEntity.of(objectMapper.writeValueAsString(
//                                            new BotActionDto(BotAction.LIST.toString(), null)
//                                    ))
//                            ).getId().toString())
            );

            response = bot.execute(new SendMessage(chatId, String.format("""
                    ***Bot name***: %s
                    """, telegramBotEntity.getName()))
                    .replyMarkup(keyboard)
                    .parseMode(ParseMode.Markdown));


            bot.execute(new AnswerCallbackQuery(callback.id()));
            bot.execute(new DeleteMessage(chatId, callback.message().messageId()));

            CallbackBotEntity callbackBotEntity = CallbackBotEntity.builder()
                    .id(UUID.randomUUID())
                    .botTokenId(bot.getToken())
                    .callbackMessageId(response.message().messageId())
                    .callbackType(CallbackType.FILES)
                    .build();

            this.callbackBotRepository.save(callbackBotEntity);
            return;
        } else if (botAction == BotAction.LIST) {

            Optional<TelegramUserEntity> admin =
                    this.telegramUserRepository.findByChatIdAndAccessToUpDaily(chatId, true);

            if (admin.isEmpty()) {
                this.telegramUserService.createIfNotExists(chatId, callback.from());
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                        new InlineKeyboardButton("Apply now")
                                .url("http://ec2-16-16-104-222.eu-north-1.compute.amazonaws.com/"),
                        new InlineKeyboardButton("Check out our demo")
                                .url("https://t.me/updaily_demo_bot")
                );
                bot.execute(new SendMessage(
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

            response = bot.execute(new SendMessage(chatId, "\uD83E\uDEC2 This your avatars:")
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard)
            );
        } else if (botAction == BotAction.CREATE) {

            response = bot.execute(new SendMessage(chatId, "\uD83E\uDEC2 This your avatars:")
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(new InlineKeyboardMarkup(
                            new InlineKeyboardButton("Аre you sure you want to create a bot?")
                                    .callbackData(this.callbackDataRepository.save(CallbackDataEntity.of(
                                            objectMapper.writeValueAsString(new BotActionDto(
                                                    BotAction.CREATE.toString(), null
                                            ))
                                    )).getId().toString())
                    ))
            );

            bot.execute(new AnswerCallbackQuery(callback.id()));
            bot.execute(new DeleteMessage(chatId, callback.message().messageId()));

            CallbackBotEntity callbackBotEntity = CallbackBotEntity.builder()
                    .id(UUID.randomUUID())
                    .botTokenId(bot.getToken())
                    .callbackMessageId(response.message().messageId())
                    .callbackType(CallbackType.BOT_CRUD)
                    .build();

            this.callbackBotRepository.save(callbackBotEntity);
        }

        bot.execute(new AnswerCallbackQuery(callback.id()));
        bot.execute(new DeleteMessage(chatId, callback.message().messageId()));

        CallbackBotEntity callbackBotEntity = CallbackBotEntity.builder()
                .id(UUID.randomUUID())
                .botTokenId(bot.getToken())
                .callbackMessageId(response.message().messageId())
                .callbackType(CallbackType.BOT_LIST)
                .build();

        this.callbackBotRepository.save(callbackBotEntity);
    }
}

