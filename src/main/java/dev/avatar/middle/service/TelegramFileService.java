package dev.avatar.middle.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.impl.FileApi;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.response.GetFileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramFileService {

    private final FileApi tgfileApi;

    public byte[] getTelegramFile(TelegramBot bot, String fileId) {
        GetFile request = new GetFile(fileId);
        GetFileResponse getFileResponse = bot.execute(request);
        if (getFileResponse.file() == null || getFileResponse.file().fileSize() > 20971520) { //todo set property limit file
            throw new RuntimeException("File error"); //todo TelegramFileException
        }
        String fileUrl = this.tgfileApi.getFullFilePath(getFileResponse.file().filePath());
        File tempFile;
        try {
            tempFile = File.createTempFile("voice_", ".oga");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            return Files.readAllBytes(tempFile.toPath());
        }
        catch (Exception e) {
            log.error("Unexpected behaviour: {}", e.getMessage());
        }
        throw new RuntimeException("File not found"); //todo TelegramFileException
    }
}
