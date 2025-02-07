package dev.avatar.middle.task;

import dev.avatar.middle.service.RetrieveVideoService;
import dev.avatar.middle.service.VideoService;
import dev.avatar.middle.service.ai.AssistantService;
import dev.avatar.middle.service.ai.HeyGenService;
import dev.avatar.middle.service.TelegramResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetrieveHeyGenResponseTask {

    private final HeyGenService heyGenService;
    private final TelegramResponseService telegramResponseService;
    private final VideoService videoService;
    private final RetrieveVideoService retrieveVideoService;

    @Scheduled(fixedRate = 3000) //todo yaml property
    public void performTask() {
        Set<Map.Entry<String, Long>> runIdWithTgChatId = this.videoService.getRunIdsQueue();
        for (Map.Entry<String, Long> entry : runIdWithTgChatId) {
            String videoRequestId = entry.getKey();
            Long telegramChatId = entry.getValue();
            this.heyGenService.checkVideoStatus(videoRequestId)
                    .subscribe(downloadUrl -> downloadUrl.ifPresent(url ->
                            this.retrieveVideoService.retrieveAndSendResponse(telegramChatId, url)
                    ));
        }
    }
}
