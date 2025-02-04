package dev.avatar.middle.service;

import dev.avatar.middle.service.ai.HeyGenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final HeyGenService heyGenService;
    private final ConcurrentHashMap<String, Long> runsQueueWithTgChatId = new ConcurrentHashMap<>();

    public void sendGenerateVideoRequest(Long chatId, String content) {
        String videoId = this.heyGenService.generateVideo(content);
        runsQueueWithTgChatId.put(videoId, chatId);
    }

    public Set<Map.Entry<String, Long>> getRunIdsQueue() {
        return this.runsQueueWithTgChatId.entrySet();
    }

    public void deleteFromQueue(Long chatId) {
        this.runsQueueWithTgChatId.remove(chatId);
    }
}
