package dev.avatar.middle.service.ai;

import dev.avatar.middle.client.ElevenLabsClient;
import dev.avatar.middle.entity.ElevenLabsData;
import dev.avatar.middle.repository.ElevenLabsDataRepository;
import dev.avatar.middle.service.TelegramResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElevenLabsService {

    private final ElevenLabsDataRepository elevenLabsDataRepository;
    private final ElevenLabsClient elevenLabsClient;
    private final TelegramResponseService responseService;

    public void generateAudioFromText(String botToken, Long chatId, String text) {
        //todo log here
        ElevenLabsData elevenLabsData =
                this.elevenLabsDataRepository.findByBotTokenId(botToken).orElseThrow(); // todo add logic exception and handler
        elevenLabsClient.generateAudio(elevenLabsData.getVoiceId(), elevenLabsData.getVoiceModelId(), text)
                .subscribe((byte[] audio) -> this.responseService.sendVoice(botToken, chatId, audio, text));

    }
}
