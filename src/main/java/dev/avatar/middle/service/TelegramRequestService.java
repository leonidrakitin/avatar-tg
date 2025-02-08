package dev.avatar.middle.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.ResponseType;
import dev.avatar.middle.service.ai.AssistantService;
import dev.avatar.middle.service.telegram.callback.TelegramCallbackProcessor;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import dev.avatar.middle.model.ChatData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramRequestService {

    private final AssistantService assistantService;
    private final TelegramFileService telegramFileService;
    private final TelegramUserService telegramUserService;
    private final ChatDataService chatDataService;
    private final List<TelegramCommand> commands;
    private final List<TelegramCallbackProcessor> callbacks;

    //todo move all bot.execute to response service
    @Async
    protected void  processRequest(TelegramBot bot, Update update) {
        if (update.callbackQuery() != null) {
            handleCallbackQuery(bot, update.callbackQuery());
        } else if (update.message() != null) {
            handleMessageUpdate(bot, update.message());
        } else {
            bot.execute(new SendMessage(update.message().chat().id(), "I am sorry, I can not process your message :("));
            log.error("Unexpected update received: {}", update);
        }
    }

    private void handleCallbackQuery(TelegramBot bot, CallbackQuery callback) {
        long chatId = callback.message().chat().id(); //todo message is deprecated
        try {
            long telegramUserId = callback.from().id();

            if (this.chatDataService.isWaitingForAnswer(chatId)) {
                bot.execute(new SendMessage(chatId, "⌛️ Please ask me later when I finish processing your previous message!")); //todo i18n
                return;
            }
            log.info("Got callback for userId {}, callback {}", telegramUserId, callback);
            this.getCallbackIfPresent(chatId).ifPresentOrElse(
                    processor -> processor.processCallback(bot, callback),
                    () -> bot.execute(new SendMessage(chatId, "Let's try again, I dont understand what are you actually want")) //todo i18n
            );
        }
        catch (Exception e) {
            log.error("Error handling callback", e);
            bot.execute(new SendMessage(chatId, "Let's try later..")); //todo i18n
        }
    }

    private void handleMessageUpdate(TelegramBot bot, Message message) {
        long chatId = message.chat().id();
        long telegramUserId = message.from().id();
        int messageId = message.messageId();
        String text = message.text();

        if (this.chatDataService.isWaitingForAnswer(chatId)) {
            bot.execute(new SendMessage(chatId, "⌛️ Please ask me later when I finish processing your previous message!"));
            return;
        }

        try {
            if (message.voice() != null) {
                processVoiceMessage(bot, messageId, message.voice().fileId(), message.from(), chatId);
            }
            else if (message.videoNote() != null) {
                processVoiceMessage(bot, messageId, message.videoNote().fileId(), message.from(), chatId);
            }
            else if (text != null) {
                Optional<TelegramCommand> command = getCommandIfPresent(text);
                command.ifPresentOrElse(
                        tgCommand -> tgCommand.processCommand(bot, chatId),
                        () -> sendRequest(bot, messageId, text, message.from(), chatId)
                );
            }
        }
        catch (Exception e) {
            log.error("Error processing message for user {}: {}", telegramUserId, e.getMessage(), e);
//            bot.execute(new SendMessage(chatId, "❌ An error occurred while processing your request."));
        }
    }

    private void processVoiceMessage(
            TelegramBot bot,
            int messageId,
            String fileId,
            User telegramUser,
            long chatId
    ) {
        byte[] fileData = this.telegramFileService.getTelegramFile(bot, fileId);
        String transcribedAudio = this.assistantService.transcriptAudio(fileData);
        log.debug("Got result from transcription audio service: {}", transcribedAudio);
        this.sendRequest(bot, messageId, transcribedAudio, telegramUser, chatId);
    }

    private void processDocument(TelegramBot bot, long telegramUserId, String fileId, String content) throws ExecutionException {
        byte[] fileData = this.telegramFileService.getTelegramFile(bot, fileId);
        this.assistantService.processDocument(bot.getToken(), telegramUserId, fileData, content);
    }

    private void sendRequest(
            TelegramBot bot,
            int messageId,
            String text,
            User telegramUser,
            long chatId
    ) {
        try {
            ResponseType responseType = this.telegramUserService.createIfNotExists(telegramUser).getResponseType();
            this.chatDataService.save(new ChatData(chatId, messageId, bot, responseType));
            this.assistantService.sendRequest(bot.getToken(), telegramUser.id(), text);
            if (responseType == ResponseType.TEXT) {
                bot.execute(new SendChatAction(chatId, ChatAction.typing));
            }
        }
        catch (ExecutionException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e); //todo add here more cases and log.errors etc
        }
    }

    private Optional<TelegramCommand> getCommandIfPresent(String text) {
        return commands.stream()
                .filter(cmd -> text.contains(cmd.getCommand()))
                .findFirst();
    }

    private Optional<TelegramCallbackProcessor> getCallbackIfPresent(Long chatId) {
        return this.chatDataService.getByChatId(chatId)
                .map(ChatData::getCallbackType)
                .map(this::getCallbackIfPresent)
                .flatMap(Function.identity());
    }

    private Optional<TelegramCallbackProcessor> getCallbackIfPresent(CallbackType type) {
        return callbacks.stream()
                .filter(callback -> type == callback.getCallbackType())
                .findFirst();
    }
}
