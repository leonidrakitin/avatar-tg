package dev.avatar.middle.service.ai;

import dev.avatar.middle.client.HeyGenClient;
import dev.avatar.middle.entity.HeyGenData;
import dev.avatar.middle.repository.HeygenDataRepository;
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

    //todo move to database
    private final ConcurrentHashMap<RequestData, String> runsQueueWithTgChatId = new ConcurrentHashMap<>();
    private final HeyGenClient heyGenClient;
    private final HeygenDataRepository heygenDataRepository;
    private final TelegramResponseService responseService;

    public Mono<Optional<String>> checkVideoStatus(String videoId) {
        log.debug("Checking video status for videoId: {}", videoId);
        return heyGenClient.getVideoStatus(videoId);
    }

    public void sendGenerateVideoRequest(String botToken, Long chatId, String content) {
        HeyGenData avatarData = this.heygenDataRepository.findByBotTokenId(botToken)
                .orElseThrow(); //todo add exception and global handler
        this.generateVideo(avatarData.getAvatarId(), avatarData.getVoiceId(), content)
                .subscribe(videoId -> runsQueueWithTgChatId.put(new RequestData(botToken, chatId), videoId));
    }

    public void retrieveAndSendResponse(String botToken, Long chatId, String downloadUrl) { //todo
        this.runsQueueWithTgChatId.remove(chatId);
        this.downloadVideo(downloadUrl)
                .subscribe((byte[] videoBytes) -> this.responseService.sendVideoNote(botToken, chatId, videoBytes));
    }

    public Set<Map.Entry<RequestData, String>> getRunIdsQueue() {
        return this.runsQueueWithTgChatId.entrySet();
    }

    private Mono<String> generateVideo(String avatarId, String voiceId, String text) {
        log.info("Generating video with text: {}", text);
        if (text.length() > 1500) {
            log.error("Text length exceeds limit: {}", text.length());
            throw new IllegalArgumentException("Text input exceeds the 1500 character limit.");
        }
        return heyGenClient.generateVideo(avatarId, voiceId, text);
    }

    private Mono<byte[]> downloadVideo(String videoUrl) {
        return heyGenClient.downloadVideo(videoUrl);
    }

    public record RequestData(String botToken, Long chatId) {} //todo final
}
