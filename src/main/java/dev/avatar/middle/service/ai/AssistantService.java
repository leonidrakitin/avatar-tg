package dev.avatar.middle.service.ai;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.avatar.middle.client.OpenAiAssistantClient;
import dev.avatar.middle.client.OpenAiAudioClient;
import dev.avatar.middle.client.OpenAiFileClient;
import dev.avatar.middle.client.dto.Data;
import dev.avatar.middle.service.ThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final ThreadService threadService;
    private final OpenAiAssistantClient openAiAssistantClient;
    private final OpenAiFileClient openAiFileClient;
    private final OpenAiAudioClient openAiAudioClient;

    //todo queue logic , to database
    private final ConcurrentHashMap<String, RequestData> runsQueueWithTgChatId = new ConcurrentHashMap<>();
    private final Cache<String, Data.Assistant> assistantCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

//    public String retrieveResponse(String runId) throws ExecutionException {
//        Data.Message = this.retrieveRun(runId).iterator().next();
//    }

    public Optional<Data.Message> retrieveResponse(String runId) throws ExecutionException {
        Long tgChatId = runsQueueWithTgChatId.get(runId).chatId();
        Data.Thread thread = this.threadService.getThreadByTelegramChatId(tgChatId);
        Data.Run runData = this.openAiAssistantClient.retrieveRun(thread.id(), runId);
        if (runData.status() != Data.Run.Status.completed) {
            return Optional.empty();
        }
        this.runsQueueWithTgChatId.remove(runId);
        Data.DataList<Data.Message> messages = openAiAssistantClient.listMessages(new Data.ListRequest(),
                thread.id());
        return messages.data().stream()
                .filter(msg -> msg.role() == Data.Role.assistant)
                .findFirst();
    }

    public byte[] retrieveFileContent(String fileId) {
        return this.openAiFileClient.retrieveFileContent(fileId);
    }

    public boolean sendRequest(String assistantId, Long tgChatId, String botToken, String message) throws ExecutionException {
        return sendRequest(assistantId, tgChatId, botToken, message, null);
    }

    public boolean sendRequest(String assistantId, Long chatId, String botToken, String message, List<Data.Attachment> attachments) throws ExecutionException {
        Data.Assistant assistant = this.getAssistant(assistantId);
        Data.Thread thread = this.threadService.getThreadByTelegramChatId(chatId);
        try {
            this.openAiAssistantClient.createMessage(
                    new Data.MessageRequest(Data.Role.user, Optional.ofNullable(message).orElse("")),
                    thread.id()
            );
            Data.Run run = this.openAiAssistantClient.createRun(thread.id(), new Data.RunRequest(assistant.id()));
            this.runsQueueWithTgChatId.put(run.id(), new RequestData(chatId, botToken));
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    public Set<Map.Entry<String, RequestData>> getRunIdsQueue() {
        return this.runsQueueWithTgChatId.entrySet();
    }

    public String transcriptAudio(byte[] file) {
        try {
            return this.openAiAudioClient.createTranscription(new OpenAiAudioClient.TranscriptionRequest(
                    file, "en", OpenAiAudioClient.TranscriptionResponseFormat.text
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void processDocument(String assistantId, Long tgChatId, byte[] file, String content) throws ExecutionException {
        String fileId = this.uploadFile(file);
        this.sendRequest(
                assistantId,
                tgChatId,
                this.runsQueueWithTgChatId.get(tgChatId).botToken(),
                content,
                List.of(new Data.Attachment(fileId, List.of(new Data.Tool(Data.Tool.Type.file_search))))
        );
    }

    public String uploadFile(byte[] file) {
        return this.openAiFileClient.uploadFile(new ByteArrayResource(file), Data.File.Purpose.ASSISTANTS).id();
    }

    private Data.Assistant getAssistant(String assistantId) throws ExecutionException {
        return this.assistantCache.get(assistantId, () -> this.loadAssistant(assistantId));
    }

    private Data.Assistant loadAssistant(String assistantId) {
        return Optional.ofNullable(this.openAiAssistantClient.retrieveAssistant(assistantId))
                .orElseThrow(() -> new RuntimeException("Assistant not found")); //todo create AssistantException
    }

    private Data.Assistant createAssistant(String botId) {

        Data.Assistant assistant = openAiAssistantClient.createAssistant(new Data.AssistantRequestBody(
                "gpt-4-1106-preview", // model
                "Math Tutor", // name
                "", // description
                "You are a personal math tutor. Write and run code to answer math questions.", // instructions
                List.of(new Data.Tool(Data.Tool.Type.code_interpreter)), // tools
                null,
                null
        )); // metadata
//        this.assistantRepository.save(AssistantEntity.of(assistant.id(), botId));
        return assistant;
    }

    public record RequestData(Long chatId, String botToken) {}
}
