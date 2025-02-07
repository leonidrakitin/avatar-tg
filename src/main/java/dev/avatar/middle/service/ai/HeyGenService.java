package dev.avatar.middle.service.ai;

import dev.avatar.middle.client.HeyGenClient;
import dev.avatar.middle.exceptions.HeyGenException;
import dev.avatar.middle.exceptions.enums.HeyGenErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Service
public class HeyGenService {

    private final HeyGenClient heyGenClient;

    public HeyGenService(HeyGenClient heyGenClient) {
        this.heyGenClient = heyGenClient;
    }

    public Mono<String> generateVideo(String text) {
        log.info("Generating video with text: {}", text);
        if (text.length() > 1500) {
            log.error("Text length exceeds limit: {}", text.length());
            throw new IllegalArgumentException("Text input exceeds the 1500 character limit.");
        }
        return heyGenClient.generateVideo(text);
    }

    public Mono<Optional<String>> checkVideoStatus(String videoId) {
        log.debug("Checking video status for videoId: {}", videoId);
        return heyGenClient.getVideoStatus(videoId);
    }

    public byte[] downloadVideo(String videoUrl) {
        log.info("Downloading video from URL: {}", videoUrl);
        return heyGenClient.downloadVideo(videoUrl);
    }
}
