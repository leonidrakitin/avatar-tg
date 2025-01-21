package dev.avatar.middle.task;

import dev.avatar.middle.service.AssistantService;
import dev.avatar.middle.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class RetrieveMessageTask {

    private final AssistantService assistantService;
    private final TelegramBotService telegramBotService;

    @Scheduled(fixedRate = 1000)
    public void performTask() throws ExecutionException {
        Set<Map.Entry<String, Long>> runIdWithTgUserId = this.assistantService.getRunIdsQueue();
        for (Map.Entry<String, Long> entry : runIdWithTgUserId) {
            String runId = entry.getKey();
            Long telegramUserId = entry.getValue();
            this.assistantService.retrieveResponse(runId).ifPresent(
                    response -> {
                        System.out.println("Retrieved run " + response);
                        String text = "";
                        byte[] image = null;
                        for (var content : response.content()) {
                            switch (content.type()) {
                                case text -> text = content.text().value();
                                case image_file -> {
                                    this.telegramBotService.sendUploadStatus(telegramUserId);
                                    image = this.assistantService.retrieveFileContent(content.image_file().file_id());
                                }
                            }
                        }
                        if (image == null) {
                            this.telegramBotService.sendMessage(telegramUserId, text);
                        } else {
                            this.telegramBotService.sendPhoto(telegramUserId, image, text);
                        }
                    }
            );
        }
    }
}
