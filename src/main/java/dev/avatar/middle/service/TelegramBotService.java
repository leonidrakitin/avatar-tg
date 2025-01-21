package dev.avatar.middle.service;

import com.logaritex.ai.api.AudioApi;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.impl.FileApi;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.LabeledPrice;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ShippingOption;
import com.pengrad.telegrambot.request.AnswerShippingQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendInvoice;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetFileResponse;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.conf.AppProperty;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.io.File;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final AppProperty properties;
    private final AssistantService openAIService;
    private FileApi tgfileApi;

//    private final Cache<Long, Long> chatsCache = CacheBuilder.newBuilder()
//            .expireAfterWrite(1, TimeUnit.HOURS)
//            .build();
    private final ConcurrentHashMap<Long, ChatData> queueTelegramIdWithChatData = new ConcurrentHashMap<>();

    private final List<TelegramBot> telegramBots = new ArrayList<>();

    @PostConstruct
    public void init() {
        tgfileApi = new FileApi(properties.getToken());
        telegramBots.add(new TelegramBot(properties.getToken()));

        for (TelegramBot bot : telegramBots) {
            bot.setUpdatesListener((List<Update> updates) -> {
                for (Update update : updates) {
                    if (update.message() != null) {
                        try {
                            if (update.message().text() != null && update.message().text().contains("/start")) {
                                bot.execute(new SetMyCommands(
                                        new BotCommand("/type", "\uD83D\uDDE3 Change communication type"),
                                        new BotCommand("/myprofile", "\uD83D\uDC64 Check my profile"),
                                        new BotCommand("/summarize", "☝\uFE0F Summarize last meeting"),
                                        new BotCommand("/language", "\uD83D\uDCAC Change language")
                                ));
                            }
                            this.processMessage(bot, update);
                        } catch (Exception e) {
                            System.out.println("Error " + e.toString());
                        }
                    }
                }
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }, e -> {
                if (e.response() != null) {
                    // got bad response from telegram
                    e.response().errorCode();
                    e.response().description();
                    log.error("Catch error: {}, description: {}", e.response().errorCode(), e.response().description());
                } else {
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
            bot.execute(new SendMessage(chatId, "⌛\uFE0F Please ask me later when I finish process your previous message!"));
            bot.execute(new SendChatAction(chatId, ChatAction.typing));
            return;
        } else {
            bot.execute(new SendChatAction(chatId, ChatAction.typing));
        }

        SendResponse sendResponse = bot.execute(new SendMessage(
                        chatId,
                        String.format(
                                "%s Your request accepted, @%s! Please wait!",
                                "\uD83D\uDC69\u200D\uD83D\uDCBB",
                                update.message().from().username()
                        )
                )
        );

        if (update.message().photo() != null) {
            System.out.println("photo");
            return;
        }
        if (update.message().document() != null) {
            try {
                String caption = Optional.ofNullable(update.message().caption()).orElse("");
                this.processDocument(bot, telegramUserId, update.message().document().fileId(), caption);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        if (sendResponse.message() == null) {
            return;
        }
        int targetMessageId = sendResponse.message().messageId();

        if (update.message().voice() != null) {
            this.processVoiceMessage(
                    bot,
                    update.message().messageId(),
                    update.message().voice().fileId(),
                    telegramUserId,
                    chatId,
                    targetMessageId
            );
        } else if (update.message().videoNote() != null) {
            this.processVoiceMessage(
                    bot,
                    update.message().messageId(),
                    update.message().videoNote().fileId(),
                    telegramUserId,
                    chatId,
                    targetMessageId
            );
        } else {
            this.sendRequest(
                    bot,
                    update.message().messageId(),
                    update.message().text(),
                    telegramUserId,
                    chatId,
                    targetMessageId
            );
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
            queueTelegramIdWithChatData.put(
                    telegramUserId,
                    new ChatData(chatId, messageId, bot, targetMessageId)
            );
            this.openAIService.sendRequest(bot.getToken(), telegramUserId, text);
            bot.execute(new SendChatAction(chatId, ChatAction.typing));
        } catch (ExecutionException e) {
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
            throw   new RuntimeException("File error");
        }
        String fileUrl = this.tgfileApi.getFullFilePath(getFileResponse.file().filePath());
        File tempFile = null;
        try {
            tempFile = File.createTempFile("voice_", ".oga");
        } catch (IOException e) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    };

    public void sendMessage(Long telegramUserId, String content) {
        Optional.ofNullable(this.queueTelegramIdWithChatData.get(telegramUserId))
                .ifPresent(chatData -> {
                    TelegramBot bot = chatData.bot();
                    this.queueTelegramIdWithChatData.remove(telegramUserId);
                    if (chatData.targetMessageId != null) {
                        bot.execute(new DeleteMessage(chatData.chatId(), chatData.targetMessageId));
                    }
                    bot.execute(
                            new SendMessage(chatData.chatId(), content)
                                    .parseMode(ParseMode.Markdown)
                                    .replyToMessageId(chatData.askMessageId())
                                    .replyMarkup(new InlineKeyboardMarkup())
                    );
                });
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

    public record ChatData(
            long chatId,
            int askMessageId,
            TelegramBot bot,
            Integer targetMessageId
    ) {}
}
