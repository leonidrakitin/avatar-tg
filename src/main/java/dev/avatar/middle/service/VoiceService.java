package dev.avatar.middle.service;

import dev.avatar.middle.service.ai.ElevenLabsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceService {

    private final ElevenLabsService elevenLabsService;

    public byte[] sendGenerateVideoRequest(Long chatId, String content) {
        byte[] voiceBytes = this.elevenLabsService.generateAudioFromText(content);
        return voiceBytes;
    }
}
