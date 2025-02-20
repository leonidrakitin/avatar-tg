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

    public static final String GPT_4_O_MINI = "gpt-4o-mini";
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

    public void deleteFile(String vectorStoreId, String fileId) {
        this.openAiFileClient.deleteFile(vectorStoreId, fileId);
        this.openAiFileClient.deleteFile(fileId);
    }

    public boolean sendRequest(String assistantId, Long chatId, String botToken, String message) throws ExecutionException {
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

    public String transcriptAudio(byte[] file, String languageCode) {
        try {
            return this.openAiAudioClient.createTranscription(new OpenAiAudioClient.TranscriptionRequest(
                    file, languageCode, OpenAiAudioClient.TranscriptionResponseFormat.text
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Data.VectorStoreList getAllFiles(String vectorStoreId, String afterId) {
        return this.openAiFileClient.retrieveVectorStoreFiles(vectorStoreId, afterId);
    }

    public Data.File getFileData(String fileId) {
        return this.openAiFileClient.retrieveFile(fileId);
    }

    public byte[] getFileContent(String fileId) {
        return this.openAiFileClient.retrieveFileContent(fileId);
    }

    public void processDocument(String vectorStoreId, String fileName, byte[] file) {
        String fileId = this.uploadFile(fileName, file);
        this.openAiFileClient.attachFileToVectorStore(vectorStoreId, fileId);
    }

    public String uploadFile(String fileName, byte[] file) {
        return this.openAiFileClient.uploadFile(
                new ByteArrayResource(file) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                },
                Data.File.Purpose.ASSISTANTS
        ).id();
    }

    private Data.Assistant getAssistant(String assistantId) throws ExecutionException {
        return this.assistantCache.get(assistantId, () -> this.loadAssistant(assistantId));
    }

    private Data.Assistant loadAssistant(String assistantId) {
        return Optional.ofNullable(this.openAiAssistantClient.retrieveAssistant(assistantId))
                .orElseThrow(() -> new RuntimeException("Assistant not found")); //todo create AssistantException
    }

    public Data.Assistant createAssistant(String vectorStoreId, String name, String description, String instructions) {

        return this.openAiAssistantClient.createAssistant(new Data.AssistantRequestBody(
                GPT_4_O_MINI,
                name,
                description,
                instructions,
                List.of(new Data.Tool(Data.Tool.Type.file_search)),
                new Data.ToolResources(new Data.FileSearch(List.of(vectorStoreId)), null),
                null
        ));
    }

    public String createVectorStore() {
        return this.openAiFileClient.createVectorStore().id();
    }

    public record RequestData(Long chatId, String botToken) {}
}
