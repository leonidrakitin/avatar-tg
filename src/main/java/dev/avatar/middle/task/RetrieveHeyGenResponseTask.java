package dev.avatar.middle.task;

import dev.avatar.middle.service.ai.HeyGenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetrieveHeyGenResponseTask {

    private final HeyGenService heyGenService;

    @Scheduled(fixedDelay = 5000)
    public void performTask() {
        Set<Map.Entry<String, HeyGenService.RequestData>> runIdWithTgChatId = this.heyGenService.getRunIdsQueue();
        for (Map.Entry<String, HeyGenService.RequestData> entry : runIdWithTgChatId) {
            Long chatId = entry.getValue().chatId();;
            String botToken = entry.getValue().botToken();
            String videoRequestId = entry.getValue().videoId();
            this.heyGenService.checkVideoStatus(videoRequestId)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .subscribe(downloadUrl -> this.heyGenService.retrieveAndSendResponse(botToken, chatId, downloadUrl));
        }
    }
}
