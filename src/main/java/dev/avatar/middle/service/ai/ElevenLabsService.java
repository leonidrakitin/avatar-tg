package dev.avatar.middle.service.ai;

import dev.avatar.middle.client.ElevenLabsClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ElevenLabsService {

    private final ElevenLabsClient elevenLabsClient;

    public ElevenLabsService(ElevenLabsClient elevenLabsClient) {
        this.elevenLabsClient = elevenLabsClient;
    }

    public Mono<byte[]> generateAudioFromText(String text) {
        return elevenLabsClient.generateAudio(text);
    }
}
