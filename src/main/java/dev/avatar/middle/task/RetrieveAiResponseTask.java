package dev.avatar.middle.task;

import dev.avatar.middle.client.dto.Data;
import dev.avatar.middle.service.AiResponseService;
import dev.avatar.middle.service.TelegramResponseService;
import dev.avatar.middle.service.ai.AssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetrieveAiResponseTask {

    private final AssistantService assistantService;
    private final AiResponseService aiResponseService;
    private final TelegramResponseService telegramResponseService;

    @Scheduled(fixedRate = 1000) //todo yaml property @Scheduled(cron = "${task.meetings-update-expired-returned-to-backoffice.cron}", zone = "Europe/Moscow")

    public void performTask() throws ExecutionException {
        Set<Map.Entry<String, AssistantService.RequestData>> runIdWithTgChatId = this.assistantService.getRunIdsQueue();
        for (Map.Entry<String, AssistantService.RequestData> entry : runIdWithTgChatId) {
            String runId = entry.getKey();
            Long chatId = entry.getValue().chatId();
            String botToken = entry.getValue().botToken();
            this.assistantService.retrieveResponse(runId).ifPresentOrElse(
                    response -> this.processResponse(response, botToken, chatId),
                    () -> this.telegramResponseService.processWaiting(botToken, chatId)
            );
        }
    }

    private void processResponse(Data.Message response, String botToken, Long chatId) {
        log.info("Retrieved run {}", response);
        String text = "";
        byte[] image = null;
        for (var content : response.content()) {
            switch (content.type()) {
                case text -> text = content.text().value();
                case image_file -> {
                    this.telegramResponseService.sendUploadStatus(botToken, chatId);
                    image = this.assistantService.retrieveFileContent(content.image_file().file_id());
                }
            }
        }
        if (image == null) {
            this.aiResponseService.sendMessage(botToken, chatId, text);
        } else {
            this.telegramResponseService.sendPhoto(botToken, chatId, image, text);
        }
    }
}
