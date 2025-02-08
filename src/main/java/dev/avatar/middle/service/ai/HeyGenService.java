package dev.avatar.middle.service.ai;

import dev.avatar.middle.client.HeyGenClient;
import dev.avatar.middle.exceptions.HeyGenException;
import dev.avatar.middle.exceptions.enums.HeyGenErrorCode;
import dev.avatar.middle.service.TelegramResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeyGenService {

    private final ConcurrentHashMap<Long, String> runsQueueWithTgChatId = new ConcurrentHashMap<>();
    private final HeyGenClient heyGenClient;
    private final TelegramResponseService responseService;

    public Mono<Optional<String>> checkVideoStatus(String videoId) {
        log.debug("Checking video status for videoId: {}", videoId);
        return heyGenClient.getVideoStatus(videoId);
    }

    public void sendGenerateVideoRequest(Long chatId, String content) {
        this.generateVideo(content).subscribe(videoId -> runsQueueWithTgChatId.put(chatId, videoId));
    }

    public void retrieveAndSendResponse(Long chatId, String downloadUrl) { //todo
        this.runsQueueWithTgChatId.remove(chatId);
        this.downloadVideo(downloadUrl)
                .subscribe((byte[] videoBytes) -> this.responseService.sendVideoNote(chatId, videoBytes));
    }

    public Set<Map.Entry<Long, String>> getRunIdsQueue() {
        return this.runsQueueWithTgChatId.entrySet();
    }

    private Mono<String> generateVideo(String text) {
        log.info("Generating video with text: {}", text);
        if (text.length() > 1500) {
            log.error("Text length exceeds limit: {}", text.length());
            throw new IllegalArgumentException("Text input exceeds the 1500 character limit.");
        }
        return heyGenClient.generateVideo(text);
    }

    private Mono<byte[]> downloadVideo(String videoUrl) {
        log.info("Downloading video from URL: {}", videoUrl);
        return heyGenClient.downloadVideo(videoUrl);
    }
}
