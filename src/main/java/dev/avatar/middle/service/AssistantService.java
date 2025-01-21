package dev.avatar.middle.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.AudioApi;
import com.logaritex.ai.api.Data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.logaritex.ai.api.FileApi;
import dev.avatar.middle.entity.AssistantEntity;
import dev.avatar.middle.repository.AssistantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final ThreadService threadService;
    private final AssistantRepository assistantRepository;
    private final AssistantApi assistantApi;
    private final FileApi fileApi;
    private final AudioApi audioApi;

    private final ConcurrentHashMap<String, Long> runsQueue = new ConcurrentHashMap<>();
    private final Cache<String, Data.Assistant> assistantCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

//    public String retrieveResponse(String runId) throws ExecutionException {
//        Data.Message = this.retrieveRun(runId).iterator().next();
//    }

    public Optional<Data.Message> retrieveResponse(String runId) throws ExecutionException {
        Long telegramUserId = runsQueue.get(runId);
        Data.Thread thread = this.threadService.getThreadByTelegramUserId(telegramUserId);
        Data.Run runData = this.assistantApi.retrieveRun(thread.id(), runId);
        if (runData.status() != Data.Run.Status.completed) {
            return Optional.empty();
        }
        this.runsQueue.remove(runId);
        Data.DataList<Data.Message> messages = assistantApi.listMessages(new Data.ListRequest(),
                thread.id());
        return messages.data().stream()
                .filter(msg -> msg.role() == Data.Role.assistant)
                .findFirst();
    }

    public byte[] retrieveFileContent(String fileId) {
        return this.fileApi.retrieveFileContent(fileId);
    }

    public void sendRequest(String botId, Long telegramUserId, String message) throws ExecutionException {
        sendRequest(botId, telegramUserId, message, null);
    }

    public void sendRequest(String botId, Long telegramUserId, String message, List<Data.Attachment> attachments) throws ExecutionException {
        Data.Assistant assistant = this.getAssistant(botId);
        Data.Thread thread = this.threadService.getThreadByTelegramUserId(telegramUserId);
        this.assistantApi.createMessage(
                new Data.MessageRequest(Data.Role.user, Optional.ofNullable(message).orElse("")),
                thread.id()
        );
        Data.Run run = this.assistantApi.createRun(thread.id(), new Data.RunRequest(assistant.id()));
        this.runsQueue.put(run.id(), telegramUserId);
    }

    public Set<Map.Entry<String, Long>> getRunIdsQueue() {
        return this.runsQueue.entrySet();
    }

    public String transcriptAudio(byte[] file) {
        try {
            return this.audioApi.createTranscription(new AudioApi.TranscriptionRequest(
                    file, "en", AudioApi.TranscriptionResponseFormat.text
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void processDocument(String botId, Long telegramUserId, byte[] file, String content) throws ExecutionException {
        String fileId = this.uploadFile(file);
        this.sendRequest(
                botId,
                telegramUserId,
                content,
                List.of(new Data.Attachment(fileId, List.of(new Data.Tool(Data.Tool.Type.file_search))))
        );
    }

    public String uploadFile(byte[] file) {
        return this.fileApi.uploadFile(new ByteArrayResource(file), Data.File.Purpose.ASSISTANTS).id();
    }

    private Data.Assistant getAssistant(String botId) throws ExecutionException {
        return this.assistantCache.get(botId, () -> this.loadAssistant(botId));
    }

    private Data.Assistant loadAssistant(String botId) {
        //AssistantEntity assistant =
        return this.assistantRepository.findByTelegramBotId(botId)
                .map(AssistantEntity::getAssistantId)
                .map(this.assistantApi::retrieveAssistant)
//                .orElse(createAssistant(botId));
                .orElseThrow();
    }

    private Data.Assistant createAssistant(String botId) {

        Data.Assistant assistant = assistantApi.createAssistant(new Data.AssistantRequestBody(
                "gpt-4-1106-preview", // model
                "Math Tutor", // name
                "", // description
                "You are a personal math tutor. Write and run code to answer math questions.", // instructions
                List.of(new Data.Tool(Data.Tool.Type.code_interpreter)), // tools
                null,
                null
        )); // metadata
        this.assistantRepository.save(AssistantEntity.of(assistant.id(), botId));
        return assistant;
    }
}
