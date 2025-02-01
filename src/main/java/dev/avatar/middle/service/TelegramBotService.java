package dev.avatar.middle.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.impl.FileApi;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideo;
import com.pengrad.telegrambot.request.SendVideoNote;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.GetFileResponse;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.conf.AppProperty;
import dev.avatar.middle.unit.enums.ResponseType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final AppProperty properties;
    private final AssistantService openAIService;
    private FileApi tgfileApi;
    private final HeyGenService heyGenService;
    private final ExecutorService videoCheckExecutor = Executors.newSingleThreadExecutor();

    //    private final Cache<Long, Long> chatsCache = CacheBuilder.newBuilder()
//            .expireAfterWrite(1, TimeUnit.HOURS)
//            .build();
    private final ConcurrentHashMap<Long, ChatData> queueTelegramIdWithChatData = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> pendingVideos = new ConcurrentHashMap<>();

    //todo add to database
    private final ConcurrentHashMap<Long, ResponseType> userWithResponseType = new ConcurrentHashMap<>();


    private final List<TelegramBot> telegramBots = new ArrayList<>();

    @PostConstruct
    public void init() {
        tgfileApi = new FileApi(properties.getToken());
        telegramBots.add(new TelegramBot(properties.getToken()));

        for (TelegramBot bot : telegramBots) {
            bot.setUpdatesListener((List<Update> updates) -> {
                for (Update update : updates) {
                    if (update.callbackQuery() != null) {
                        handleCallbackQuery(bot, update.callbackQuery());
                    }
                    if (update.message() != null) {
                        try {
                            if (update.message().text() != null && update.message().text().contains("/start")) {
                                bot.execute(new SetMyCommands(
                                        new BotCommand("/type", "\uD83D\uDDE3 Change communication type"),
                                        new BotCommand("/myprofile", "\uD83D\uDC64 Check my profile"),
                                        new BotCommand("/summarize", "☝\uFE0F Summarize last meeting"),
                                        new BotCommand("/language", "\uD83D\uDCAC Change language")
                                ));

                                // Создаем клавиатуру с тремя кнопками
                                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                                        new InlineKeyboardButton("TEXT").callbackData(ResponseType.TEXT.toString()),
                                        new InlineKeyboardButton("VIDEO").callbackData(ResponseType.VIDEO.toString()));

                                // Отправляем приветственное сообщение с кнопками
                                bot.execute(
                                        new SendMessage(update.message().chat().id(), "Hi! Pick a format for my answers. I can reply to you in text, voice messages, and Circle Video format. You can always change this by pressing the \"menu\" button.")
                                        .replyMarkup(keyboard));
                            }
                            else {
                                this.processMessage(bot, update);
                            }
                        }
                        catch (Exception e) {
                            System.out.println("Error " + e.toString());
                        }
                    }
                }
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }, e -> {
                if (e.response() != null) {
                    e.response().errorCode();
                    e.response().description();
                    log.error("Catch error: {}, description: {}", e.response().errorCode(), e.response().description());
                }
                else {
                    // probably network error
                    e.printStackTrace();
                }
            });
        }
    }

    @Async
    protected void processMessage(TelegramBot bot, Update update) {
        long chatId = update.message().chat().id();
        long telegramUserId = update.message().from().id();

        if (queueTelegramIdWithChatData.containsKey(telegramUserId)) {
            bot.execute(new SendMessage(chatId, "⌛️ Please ask me later when I finish processing your previous message!"));
            return;
        }

        ChatData existingData = queueTelegramIdWithChatData.getOrDefault(
                telegramUserId,
                new ChatData(chatId, update.message().messageId(), bot, null, userWithResponseType.getOrDefault(telegramUserId, ResponseType.TEXT)) // TEXT по умолчанию
        );

        queueTelegramIdWithChatData.put(telegramUserId, existingData);

        try {
            SendResponse sendResponse = bot.execute(new SendMessage(
                    chatId,
                    String.format("💻 Your request has been accepted, @%s! Please wait...",
                            update.message().from().username())
            ));

            int targetMessageId = sendResponse.message() != null ? sendResponse.message().messageId() : 0;

            if (update.message().voice() != null) {
                processVoiceMessage(
                        bot,
                        update.message().messageId(),
                        update.message().voice().fileId(),
                        telegramUserId,
                        chatId,
                        targetMessageId
                );
            }
            else if (update.message().videoNote() != null) {
                processVoiceMessage(bot,
                        update.message().messageId(),
                        update.message().videoNote().fileId(),
                        telegramUserId,
                        chatId,
                        targetMessageId
                );
            }
            else if (update.message().text() != null) {
                sendRequest(
                        bot,
                        update.message().messageId(),
                        update.message().text(),
                        telegramUserId,
                        chatId,
                        targetMessageId
                );
            }
        }
        catch (Exception e) {
            log.error("Error processing message for user {}: {}", telegramUserId, e.getMessage());
            bot.execute(new SendMessage(chatId, "❌ An error occurred while processing your request."));
        }
        finally {
            // Удаляем пользователя из очереди после обработки
            queueTelegramIdWithChatData.remove(telegramUserId);
        }
    }

    private void sendRequest(
            TelegramBot bot,
            int messageId,
            String text,
            long telegramUserId,
            long chatId,
            int targetMessageId
    ) {
        try {
            // Проверяем существующий ChatData или создаем новый с TEXT по умолчанию
            ChatData existingData = queueTelegramIdWithChatData.getOrDefault(
                    telegramUserId,
                    new ChatData(chatId, messageId, bot, targetMessageId, ResponseType.TEXT) // Установлено значение по умолчанию
            );

            // Создаем ChatData с учетом существующих данных
            ChatData newChatData = new ChatData(
                    chatId,
                    messageId,
                    bot,
                    targetMessageId,
                    existingData.responseType() // Используем текущий responseType
            );

            // Обновляем данные пользователя
            queueTelegramIdWithChatData.put(telegramUserId, newChatData);

            this.openAIService.sendRequest(bot.getToken(), telegramUserId, text);
            bot.execute(new SendChatAction(chatId, ChatAction.typing));
        }
        catch (ExecutionException e) {
            queueTelegramIdWithChatData.remove(telegramUserId);
            throw new RuntimeException(e);
        }
    }

    private void processDocument(TelegramBot bot, long telegramUserId, String fileId, String content) throws ExecutionException {
        byte[] fileData = this.getTelegramFile(bot, fileId).orElseThrow(() -> new RuntimeException("File not found"));
        this.openAIService.processDocument(bot.getToken(), telegramUserId, fileData, content);
    }

    private void processVoiceMessage(
            TelegramBot bot,
            int messageId,
            String fileId,
            long telegramUserId,
            long chatId,
            int targetMessageId
    ) {
        byte[] fileData = this.getTelegramFile(bot, fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        var result = this.openAIService.transcriptAudio(fileData);
        System.out.println(result);
        this.sendRequest(bot, messageId, result, telegramUserId, chatId, targetMessageId);
    }

    private Optional<byte[]> getTelegramFile(TelegramBot bot, String fileId) {
        GetFile request = new GetFile(fileId);
        GetFileResponse getFileResponse = bot.execute(request);
        if (getFileResponse.file() == null || getFileResponse.file().fileSize() > 20971520) { //todo
            throw new RuntimeException("File error");
        }
        String fileUrl = this.tgfileApi.getFullFilePath(getFileResponse.file().filePath());
        File tempFile = null;
        try {
            tempFile = File.createTempFile("voice_", ".oga");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            return Optional.of(Files.readAllBytes(tempFile.toPath()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    ;

    public void sendMessage(Long telegramUserId, String content) {
        ChatData chatData = this.queueTelegramIdWithChatData.get(telegramUserId);
        if (chatData == null) {
            return;
        }

        try {
            if (chatData.responseType() == ResponseType.VIDEO) {
                String videoId = heyGenService.generateVideo(content);
                pendingVideos.put(videoId, telegramUserId);
                startVideoStatusChecker(videoId, telegramUserId);
                chatData.bot().execute(new SendMessage(chatData.chatId(), "⏳ Video is being generated..."));
            }
            else {
                TelegramBot bot = chatData.bot();
                if (chatData.targetMessageId() != null) {
                    bot.execute(new DeleteMessage(chatData.chatId(), chatData.targetMessageId()));
                }
                bot.execute(
                        new SendMessage(chatData.chatId(), content)
                                .parseMode(ParseMode.Markdown)
                                .replyToMessageId(chatData.askMessageId())
                );
            }
        }
        finally {
            queueTelegramIdWithChatData.remove(telegramUserId);
        }
    }

    public void sendPhoto(Long telegramUserId, byte[] photo, String caption) {
        Optional.ofNullable(this.queueTelegramIdWithChatData.get(telegramUserId))
                .ifPresent(chatData -> {
                    TelegramBot bot = chatData.bot();
                    this.queueTelegramIdWithChatData.remove(telegramUserId);
                    if (chatData.targetMessageId != null) {
                        bot.execute(new DeleteMessage(chatData.chatId(), chatData.targetMessageId));
                    }
                    bot.execute(
                            new SendPhoto(chatData.chatId(), photo)
                                    .caption(caption)
                                    .replyToMessageId(chatData.askMessageId())
                                    .replyMarkup(new InlineKeyboardMarkup())
                    );
                });
    }

    public void sendUploadStatus(Long telegramUserId) {
        Optional.ofNullable(this.queueTelegramIdWithChatData.get(telegramUserId))
                .ifPresent(chatData ->
                        chatData.bot().execute(new SendChatAction(chatData.chatId(), ChatAction.upload_photo))
                );
    }
//    private String formatContent(String content) {
//        if (content.contains("*"))
//    }

    public void sendVideoNote(Long telegramUserId, byte[] videoBytes, String caption) {
        Optional.ofNullable(this.queueTelegramIdWithChatData.get(telegramUserId))
                .ifPresent(chatData -> {
                    TelegramBot bot = chatData.bot();
                    this.queueTelegramIdWithChatData.remove(telegramUserId);
                    if (chatData.targetMessageId() != null) {
                        bot.execute(new DeleteMessage(chatData.chatId(), chatData.targetMessageId()));
                    }
                    bot.execute(
                            new SendVideoNote(chatData.chatId(), videoBytes)
                                    .replyToMessageId(chatData.askMessageId())
                    );
                });
    }

    //todo @Async
    private void startVideoStatusChecker(String videoId, Long telegramUserId) {
        videoCheckExecutor.submit(() -> {
            while (true) {
                try {
                    Optional<String> videoUrl = heyGenService.checkVideoStatus(videoId);
                    if (videoUrl.isPresent()) {
                        byte[] videoBytes = heyGenService.downloadVideo(videoUrl.get());
                        sendVideoNote(telegramUserId, videoBytes, "Here's your video!");
                        pendingVideos.remove(videoId);
                        break;
                    }
                    Thread.sleep(5000);
                }
                catch (Exception e) {
                    log.error("Video status check failed", e);
                    break;
                }
                finally {
                    // Удаляем пользователя из очереди
                    queueTelegramIdWithChatData.remove(telegramUserId);
                }
            }
        });
    }

    private void handleCallbackQuery(TelegramBot bot, CallbackQuery callback) {
        try {
            ResponseType responseType = ResponseType.valueOf(callback.data());
            long chatId = callback.message().chat().id();
            long userId = callback.from().id();

            // Получаем существующий ChatData или создаем новый
            ChatData existingData = queueTelegramIdWithChatData.get(userId);

            ChatData newChatData = new ChatData(
                    chatId,
                    existingData != null ? existingData.askMessageId() : callback.message().messageId(), // todo optional.ofNullable().orElseGet();
                    bot,
                    existingData != null ? existingData.targetMessageId() : null // ????
            );
            queueTelegramIdWithChatData.put(userId, newChatData);

            // sets type
            userWithResponseType.put(userId, responseType);

            // Отправляем сообщение об успешном изменении формата
            bot.execute(new AnswerCallbackQuery(callback.id()));
            bot.execute(new SendMessage(chatId, "✅ Response type set to: " + responseType));

            // **Важно**: Удаляем пользователя из очереди после смены формата
            queueTelegramIdWithChatData.remove(userId);

        }
        catch (Exception e) {
            log.error("Error handling callback", e);
        }
    }


    public record ChatData(
            long chatId,
            int askMessageId,
            TelegramBot bot,
            Integer targetMessageId,
            ResponseType responseType
    ) {
    }
}
