//package dev.avatar.middle.service.telegram.command.godfather;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
//import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
//import com.pengrad.telegrambot.request.SendMessage;
//import com.pengrad.telegrambot.response.SendResponse;
//import dev.avatar.middle.entity.CallbackBotEntity;
//import dev.avatar.middle.entity.CallbackDataEntity;
//import dev.avatar.middle.model.Bot;
//import dev.avatar.middle.model.CallbackType;
//import dev.avatar.middle.model.FilesAction;
//import dev.avatar.middle.model.TelegramBotType;
//import dev.avatar.middle.repository.CallbackBotRepository;
//import dev.avatar.middle.repository.CallbackDataRepository;
//import dev.avatar.middle.service.telegram.command.TelegramCommand;
//import dev.avatar.middle.service.telegram.command.dto.FileActionDto;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
//import java.util.UUID;
//
//@Component
//@RequiredArgsConstructor
//public class FilesCommand implements TelegramCommand {
//
//    private final ObjectMapper objectMapper;
//    private final CallbackBotRepository callbackBotRepository;
//    private final CallbackDataRepository callbackDataRepository;
//
//    @Override
//    public TelegramBotType getBotType() {
//        return TelegramBotType.GODFATHER_BOT;
//    }
//
//    @Override
//    public String getDescription() {
//        return "Start bot command";
//    } //todo i18n
//
//    @Override
//    public String getCommand() {
//        return "/files";
//    }
//
//    @Override
//    public void processCommand(Bot telegramBot, Long chatId) throws JsonProcessingException {
//
//        String vectorStoreId = "vs_nBKshcDQFWNMpwWcxRwFj3yf";
//
//        String callbackData = objectMapper.writeValueAsString(
//                new FileActionDto(FilesAction.LIST.toString(), null, vectorStoreId)
//        );
//        String callbackId = this.callbackDataRepository.save(CallbackDataEntity.of(callbackData)).getId().toString();
//
//        SendResponse response = telegramBot.getExecutableBot().execute(new SendMessage(chatId, "open files")
//                .replyMarkup(new InlineKeyboardMarkup()
//                        .addRow(new InlineKeyboardButton("Open")
//                                .callbackData(callbackId)
//                        )
//                )
//        );
//        CallbackBotEntity callbackBotEntity = CallbackBotEntity.builder()
//                .id(UUID.randomUUID())
//                .botTokenId(telegramBot.getToken())
//                .callbackMessageId(response.message().messageId())
//                .callbackType(CallbackType.FILES)
//                .build();
//        this.callbackBotRepository.save(callbackBotEntity);
//    }
//}
