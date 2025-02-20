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
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import dev.avatar.middle.client.dto.Data;
import dev.avatar.middle.entity.CallbackBotEntity;
import dev.avatar.middle.entity.CallbackDataEntity;
import dev.avatar.middle.model.CallbackType;
import dev.avatar.middle.model.FilesAction;
import dev.avatar.middle.repository.CallbackBotRepository;
import dev.avatar.middle.repository.CallbackDataRepository;
import dev.avatar.middle.service.ChatDataService;
import dev.avatar.middle.service.ai.AssistantService;
import dev.avatar.middle.service.telegram.command.dto.FileActionDto;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Component
public class FilesCallbackProcessor extends TelegramCallbackProcessor {

    private final AssistantService assistantService;
    private final CallbackBotRepository callbackBotRepository;
    private final CallbackDataRepository callbackDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FilesCallbackProcessor(
            ChatDataService chatDataService,
            AssistantService assistantService,
            CallbackBotRepository callbackBotRepository,
            CallbackDataRepository callbackDataRepository
    ) {
        super(chatDataService);
        this.assistantService = assistantService;
        this.callbackBotRepository = callbackBotRepository;
        this.callbackDataRepository = callbackDataRepository;
    }

    @Override
    public CallbackType getCallbackType() {
        return CallbackType.FILES;
    }

    @Override
    public void process(TelegramBot bot, CallbackQuery callback) throws JsonProcessingException {
        long chatId = callback.message().chat().id();
        CallbackDataEntity callbackData = this.callbackDataRepository.findById(UUID.fromString(callback.data()))
                .orElseThrow(() -> new RuntimeException("Unexpected behaviour"));
        FileActionDto fileActionDto = objectMapper.readValue(callbackData.getData(), FileActionDto.class);
        String vectorStoreId = fileActionDto.vectorStoreId();
        FilesAction filesAction = FilesAction.valueOf(fileActionDto.action());
        SendResponse response = null;

        if (filesAction == FilesAction.LIST) {
            Data.VectorStoreList responseVectorStore = this.assistantService.getAllFiles(vectorStoreId, fileActionDto.fileId());
            StringBuilder text = new StringBuilder("\uD83D\uDCDA Your assistant's knowledge base files:");
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            if (responseVectorStore.data().isEmpty()) {
                text.append("\n***No assistant knowledge base files***");
            } else {
                responseVectorStore.data().stream()
                        .map(Data.Id::id)
                        .map(this.assistantService::getFileData)
                        .forEach(data -> {
                            try {
                                keyboard.addRow(
                                        new InlineKeyboardButton(data.filename())
                                                .callbackData(this.callbackDataRepository.save(
                                                        CallbackDataEntity.of(objectMapper.writeValueAsString(
                                                                new FileActionDto(
                                                                        FilesAction.GET.toString(),
                                                                        data.id(),
                                                                        vectorStoreId
                                                                )
                                                        )
                                                )).getId().toString())
                                );
                            } catch (JsonProcessingException e) {
                                log.error(e.getMessage());
                            }
                        });
            }

            keyboard.addRow(new InlineKeyboardButton("\uD83C\uDD95 Upload new")
                    .callbackData(this.callbackDataRepository.save(
                            CallbackDataEntity.of(objectMapper.writeValueAsString(
                                    new FileActionDto(FilesAction.UPLOAD.toString(), null, vectorStoreId)
                            ))
                    ).getId().toString())
            );
            if (responseVectorStore.has_more()) {
                FileActionDto loadMoreData = new FileActionDto(
                        FilesAction.LIST.toString(),
                        responseVectorStore.last_id(),
                        vectorStoreId
                );
                String loadMoreDataJson = this.callbackDataRepository.save(CallbackDataEntity.of(
                        objectMapper.writeValueAsString(loadMoreData)
                )).getId().toString();
                keyboard.addRow(new InlineKeyboardButton("\uD83D\uDD3D Load more").callbackData(loadMoreDataJson));
            }

            if (fileActionDto.fileId() != null) {
                keyboard.addRow(getFilesBackButton(vectorStoreId));
            }

            response = bot.execute(new SendMessage(chatId, text.toString())
                    .replyMarkup(keyboard)
                    .parseMode(ParseMode.Markdown));
        } else if (filesAction == FilesAction.GET || filesAction == FilesAction.DOWNLOAD) {
            String fileId = fileActionDto.fileId();
            Data.File dataFile = this.assistantService.getFileData(fileId);
            InlineKeyboardMarkup get_keyboard = new InlineKeyboardMarkup(
//                    new InlineKeyboardButton("\uD83D\uDD17 Download file")
//                            .callbackData(this.callbackDataRepository.save(
//                                    CallbackDataEntity.of(objectMapper.writeValueAsString(
//                                            new FileActionDto(FilesAction.DOWNLOAD.toString(), fileId, vectorStoreId)
//                                    ))
//                            ).getId().toString()),
                    new InlineKeyboardButton("\uD83D\uDDD1 Delete file")
                            .callbackData(this.callbackDataRepository.save(
                                    CallbackDataEntity.of(objectMapper.writeValueAsString(
                                            new FileActionDto(FilesAction.DELETE.toString(), fileId, vectorStoreId)
                                    ))
                            ).getId().toString()),
                    getFilesBackButton(vectorStoreId)
            );
            double fileSize = dataFile.bytes() / (1024.0);
            response = bot.execute(new SendMessage(chatId, String.format("""
                    📂%s
                    ***Created at:*** %s
                    ***File size:*** %.1f kb
                    """,
                    dataFile.filename(),
                    Instant.ofEpochSecond(dataFile.created_at()).atZone(ZoneId.systemDefault()).toLocalDate(),
                    fileSize
            ))
                    .replyMarkup(get_keyboard)
                    .parseMode(ParseMode.Markdown));
        } else if (filesAction == FilesAction.DELETE) {
            String fileId = fileActionDto.fileId();
            this.assistantService.deleteFile(vectorStoreId, fileId);
            InlineKeyboardMarkup deleted_keyboard = new InlineKeyboardMarkup(
                    getFilesBackButton(vectorStoreId)
            );
            response = bot.execute(new SendMessage(chatId, String.format("✅ Successfully deleted file!"))
                    .replyMarkup(deleted_keyboard)
                    .parseMode(ParseMode.Markdown));
        } else if (filesAction == FilesAction.UPLOAD) {
            response = bot.execute(new SendMessage(chatId, "\uD83D\uDC47 Please upload the file")
                    .parseMode(ParseMode.Markdown));
        } else {
            throw new RuntimeException("Unexpected behaviour, files action does not exists");
        }

        if (filesAction == FilesAction.DOWNLOAD) {
            String fileId = fileActionDto.fileId();
            byte[] file = this.assistantService.getFileContent(fileId);
            bot.execute(new SendDocument(chatId, file));
        }

        bot.execute(new AnswerCallbackQuery(callback.id()));
        bot.execute(new DeleteMessage(chatId, callback.message().messageId()));

        CallbackBotEntity callbackBotEntity = CallbackBotEntity.builder()
                .id(UUID.randomUUID())
                .botTokenId(bot.getToken())
                .callbackMessageId(response.message().messageId())
                .callbackType(CallbackType.FILES)
                .build();

        this.callbackBotRepository.save(callbackBotEntity);
    }

    @NotNull
    private InlineKeyboardButton getFilesBackButton(String vectorStoreId) throws JsonProcessingException {
        return new InlineKeyboardButton("↩ Back")
                .callbackData(this.callbackDataRepository.save(
                        CallbackDataEntity.of(objectMapper.writeValueAsString(
                                new FileActionDto(FilesAction.LIST.toString(), null, vectorStoreId)
                        ))
                ).getId().toString());
    }
}
