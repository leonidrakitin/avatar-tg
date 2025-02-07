package dev.avatar.middle.service;

import dev.avatar.middle.service.ai.HeyGenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieveVideoService {

    private final HeyGenService heyGenService;
    private final TelegramResponseService telegramResponseService;
    private final VideoService videoService;

    @Async
    public void retrieveAndSendResponse(Long chatId, String downloadUrl) {
        this.videoService.deleteFromQueue(chatId);
        byte[] videoBytes = this.heyGenService.downloadVideo(downloadUrl);
        this.telegramResponseService.sendVideoNote(chatId, videoBytes, "Here's your video!"); //todo add text from Open AI response, текст который озвучивается
    }
}
