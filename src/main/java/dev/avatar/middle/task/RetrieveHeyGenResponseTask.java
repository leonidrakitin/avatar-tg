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

    @Scheduled(fixedRate = 3000) //todo yaml property
    public void performTask() {
        Set<Map.Entry<Long, String>> runIdWithTgChatId = this.heyGenService.getRunIdsQueue();
        for (Map.Entry<Long, String> entry : runIdWithTgChatId) {
            Long chatId = entry.getKey();
            String videoRequestId = entry.getValue();
            this.heyGenService.checkVideoStatus(videoRequestId)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .subscribe(downloadUrl -> this.heyGenService.retrieveAndSendResponse(chatId, downloadUrl));
        }
    }
}
