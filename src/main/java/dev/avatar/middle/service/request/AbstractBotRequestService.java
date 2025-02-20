package dev.avatar.middle.service.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.entity.CallbackBotEntity;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.CallbackBotRepository;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.telegram.callback.TelegramCallbackProcessor;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
abstract public class AbstractBotRequestService {

    private final ChatDataService chatDataService;
    private final CallbackBotRepository callbackBotRepository;
    private final List<TelegramCallbackProcessor> callbacks;

    //todo move all bot.execute to response service
    @Async
    public void processRequest(Bot bot, List<Update> updates) throws JsonProcessingException {
        for (Update update : updates) {
            if (isPossibleCommand(update.message())) {
                Optional<TelegramCommand> cmd = this.getCommandIfPresent(bot.getCommands(), update.message().text());
                if (cmd.isPresent()) {
                    if (!cmd.get().getCommand().equals("/cancel")) {
                        if (this.chatDataService.isWaitingForAnswer(bot.getToken(), update.message().chat().id())) {
                            bot.getExecutableBot().execute(new SendMessage( //todo implement method sendMessage
                                    update.message().chat().id(),
                                    "⌛️ Please ask me later when I finish processing your previous message!"
                            )); //todo i18n, todo create separate method with this logic
                            return;
                        }
                    }
                    cmd.get().processCommand(bot, update.message().chat().id());
                    return;
                }
            }
            if (update.callbackQuery() != null) {
                handleCallbackQuery(bot.getExecutableBot(), update.callbackQuery());
            } else if (update.message() != null) {
                handleMessageUpdate(bot, update.message());
            } else if (update.message() != null && update.message().chat() != null && update.message().chat().id() != null) {
                bot.getExecutableBot().execute(new SendMessage(
                        update.message().chat().id(),
                        "I am sorry, I can not process your message :(")
                ); //todo i18n
                log.error("Unexpected update received: {}", update);
            }
        }
    }

    public abstract void handleMessageUpdate(Bot bot, Message message);

    public abstract TelegramBotType getSupportedBotType();

    private void handleCallbackQuery(TelegramBot bot, CallbackQuery callback) {
        long chatId = callback.message().chat().id();
        long callbackMessageId = callback.message().messageId();
        try {
            long telegramUserId = callback.from().id();

            if (this.chatDataService.isWaitingForAnswer(bot.getToken(), chatId)) {
                bot.execute(new SendMessage(chatId, "⌛️ Please ask me later when I finish processing your previous message!")); //todo i18n
                return;
            }
            log.info("Got callback for userId {}, callback {}", telegramUserId, callback);
            Optional<CallbackBotEntity> callbackBotEntity =
                    this.callbackBotRepository.findByCallbackMessageIdAndAndBotTokenId(
                    callbackMessageId, bot.getToken()
            );
            if (callbackBotEntity.isEmpty()) {
                bot.execute(new SendMessage(chatId, "Please recall command to choose again"));
            }
            this.getCallbackIfPresent(callbackBotEntity)
                    .ifPresentOrElse(
                            processor -> {
                                try {
                                    processor.processCallback(bot, callback);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            () -> bot.execute(new SendMessage(chatId, "Please recall command to choose again"))
                    );
        }
        catch (Exception e) {
            log.error("Error handling callback", e);
            bot.execute(new SendMessage(chatId, "Let's try later..")); //todo i18n
        }
    }

    private Optional<TelegramCallbackProcessor> getCallbackIfPresent(Optional<CallbackBotEntity> callbackBotEntity) {
        return callbackBotEntity
                .map(CallbackBotEntity::getCallbackType)
                .map(this::getCallbackIfPresent)
                .flatMap(Function.identity());
    }

    private Optional<TelegramCallbackProcessor> getCallbackIfPresent(CallbackType type) {
        return callbacks.stream()
                .filter(callback -> type == callback.getCallbackType()) //todo check bots type??
                .findFirst();
    }

    private Optional<TelegramCommand> getCommandIfPresent(List<TelegramCommand> commands, String text) {
        return commands.stream()
                .filter(cmd -> text.contains(cmd.getCommand()))
                .findFirst();
    }

    private boolean isPossibleCommand(Message message) {
        return message != null && message.text() != null;
    }
}
