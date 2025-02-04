package dev.avatar.middle.task;

import com.logaritex.ai.api.Data;
import dev.avatar.middle.service.ai.AssistantService;
import dev.avatar.middle.service.TelegramResponseService;
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
    private final TelegramResponseService telegramResponseService;

    @Scheduled(fixedRate = 1000) //todo yaml property @Scheduled(cron = "${task.meetings-update-expired-returned-to-backoffice.cron}", zone = "Europe/Moscow")

    public void performTask() throws ExecutionException {
        Set<Map.Entry<String, Long>> runIdWithTgChatId = this.assistantService.getRunIdsQueue();
        for (Map.Entry<String, Long> entry : runIdWithTgChatId) {
            String runId = entry.getKey();
            Long telegramChatId = entry.getValue();
            this.assistantService.retrieveResponse(runId).ifPresentOrElse(
                    response -> this.processResponse(response, telegramChatId),
                    () -> this.telegramResponseService.processWaiting(telegramChatId)
            );
        }
    }

    private void processResponse(Data.Message response, Long telegramChatId) {
        log.info("Retrieved run {}", response);
        String text = "";
        byte[] image = null;
        for (var content : response.content()) {
            switch (content.type()) {
                case text -> text = content.text().value();
                case image_file -> {
                    this.telegramResponseService.sendUploadStatus(telegramChatId);
                    image = this.assistantService.retrieveFileContent(content.image_file().file_id());
                }
            }
        }
        if (image == null) {
            this.telegramResponseService.sendMessage(telegramChatId, text);
        } else {
            this.telegramResponseService.sendPhoto(telegramChatId, image, text);
        }
    }
}
